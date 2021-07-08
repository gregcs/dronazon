package domain.drone;

import domain.drone.base.Drone;
import domain.drone.base.DroneServiceImpl;
import domain.drone.services.*;
import domain.exceptions.InvalidPosition;
import domain.exceptions.NotRegisteredDrone;
import domain.order.Order;
import domain.other.SyncList;
import domain.other.WebServiceClient;
import domain.position.Position;
import domain.sensor.MeasurementBuffer;
import grpc.domain.DroneServiceOuterClass.OrderResponse;
import settings.Config;
import utils.Utils;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;

public class WorkerDrone extends Drone {

    private final String administratorServerAddress;

    private final SyncList<Drone> otherDrones;

    private final SyncList<Double> measurementMeans = new SyncList<>();

    private double kmTraveled;
    private final Object lockKmTraveled = new Object();

    private int deliveryCounter;
    private final Object lockDeliveryCounter = new Object();

    private boolean electionParticipant;
    private final Object lockElectionParticipant = new Object();

    private boolean inElection;
    private final Object lockInElection = new Object();

    private boolean quit;
    private final Object lockQuit = new Object();

    private DroneService droneService;
    private BroadcastPresentationService broadcastPresentationService;
    private SensorService sensorService;
    private PingService pingService;
    private PrintInformationService printInformationService;
    private MasterService masterService;

    public WorkerDrone(int id, String ipAddress, int portToDrones,
                       Position position, String administratorServerAddress,
                       List<Drone> otherDrones) {
        super(id, ipAddress, portToDrones, position);
        this.otherDrones = new SyncList<>(otherDrones);
        this.administratorServerAddress = administratorServerAddress;
    }

    public static WorkerDrone initialize(int idUpperBound, String hostName, String administratorServerAddress) throws IOException, InvalidPosition {
        int id = Utils.readIntegerDefault(
                "Enter the drone ID",
                "INVALID ID", 1, idUpperBound);

        int portToDrones = Utils.readIntegerDefault(
                "Enter the listening port for communication between drones",
                "INVALID PORT NUMBER", 49152, 65536);

        return new WorkerDrone(id, hostName, portToDrones, new Position(0,0), administratorServerAddress, null);
    }

    public String getAdministratorServerAddress() { return administratorServerAddress; }

    public List<Drone> getOtherDrones(){ return otherDrones.getAll(); }

    public void setOtherDrones(List<Drone> otherDrones){ this.otherDrones.setList(otherDrones); }

    public void addOtherDrone(Drone drone){ otherDrones.add(drone); }

    public void removeOtherDrone(Drone drone){ otherDrones.remove(drone); }

    public void addMeasurementMean(Double measurementMean){ measurementMeans.add(measurementMean); }

    public double getKmTraveled(){
        synchronized(lockKmTraveled){
            return kmTraveled;
        }
    }

    private void updateKmTraveled(double km) {
        synchronized(lockKmTraveled){
            kmTraveled += km;
        }
    }

    public int getDeliveryCounter() {
        synchronized(lockDeliveryCounter){
            return deliveryCounter;
        }
    }

    private void incrementDeliveryCounter() {
        synchronized(lockDeliveryCounter){
            deliveryCounter +=  1;
        }
    }

    //QUIT
    public boolean getQuit(){
        synchronized(lockQuit){
            return quit;
        }
    }

    public void setQuitAndNotify(){
        synchronized(lockQuit){
            quit = true;
            lockQuit.notifyAll();
        }
    }

    public void waitQuit() throws InterruptedException {
        synchronized(lockQuit){
            lockQuit.wait();
        }
    }

    public void quit() throws InterruptedException {
        synchronized(lockQuit){
            if(!getQuit()){
                if(getMaster()){
                    masterService.quit();
                }else{
                    waitToCompleteOrder(Config.DEFAULT_GRPC_TIMEOUT);
                    waitElectionAndQuitDroneService();
                    exitFromAdministratorServer(Config.ADMINISTRATOR_SERVER_ADDRESS);
                    quitSensorService();
                    quitPrintInformationService();
                    setQuitAndNotify();
                }
            }
        }
    }
    //END QUIT

    //ORDER
    public boolean lowBattery(){
        return getBatteryLevel() <= Config.PERCENTAGE_BATTERY_LIMIT_TO_QUIT;
    }

    public void lowBatteryQuit() throws InterruptedException {
        if(lowBattery()){
            quit();
        }
    }

    public void waitToCompleteOrder(long time) throws InterruptedException { waitIfBusy(time); }

    public void orderArrived() throws InterruptedException { setBusyOrWait(); }

    public void orderCompleted() throws InterruptedException {
        if(!getMaster()){
            notifyNotBusy();
            lowBatteryQuit();
        }
    }

    public OrderResponse delivery(Order order) throws InterruptedException {
        double kmTraveled = Position.distance(getPosition(), order.pickupPosition) + Position.distance(order.pickupPosition, order.deliveryPosition);
        Thread.sleep(Config.SLEEP_TIME_DELIVERY);
        long arrivalTimestamp = new Timestamp(System.currentTimeMillis()).getTime();
        setPosition(order.deliveryPosition);
        updateBatteryLevel();
        updateKmTraveled(kmTraveled);
        incrementDeliveryCounter();
        return DroneServiceImpl.buildOrderResponse(getId(), arrivalTimestamp, getPosition(),
                kmTraveled, measurementMeans.removeAll(), getBatteryLevel());
    }
    //ORDER

    //ELECTION
    public void setElectionParticipant(boolean electionParticipant){
        synchronized(lockElectionParticipant){
            this.electionParticipant = electionParticipant;
        }
    }

    public boolean getElectionParticipant(){
        synchronized(lockElectionParticipant){
            return electionParticipant;
        }
    }

    public void setInElection(boolean inElection){
        synchronized(lockInElection){
            this.inElection = inElection;
            if(!inElection){
                lockInElection.notifyAll();
            }
        }
    }

    public void waitElectionAndQuitDroneService() throws InterruptedException {
        synchronized (lockInElection){
            if(inElection){
                lockInElection.wait();
            }
            if(pingService != null) pingService.interrupt();
            quitDroneService();
        }
    }

    public Drone getOtherDroneMaster(){
        Drone ret = null;
        synchronized(otherDrones){
            for (Drone d: otherDrones.getAll()) {
                if(d.getMaster()){
                    ret = d;
                    break;
                }
            }
        }
        return  ret;
    }

    public void setOtherDroneMaster(int idDrone){
        synchronized(otherDrones){
            Drone actualMasterDrone = getOtherDroneMaster();
            if(actualMasterDrone != null && actualMasterDrone.getId() != idDrone)
                removeOtherDrone(actualMasterDrone);
            for(Drone d: otherDrones.getAll()){
                if(d.getId() == idDrone){
                        d.setMaster(true);
                        otherDrones.notifyAll();
                        break;
                }
            }
        }
    }

    public void waitDroneMaster(long time) throws InterruptedException {
        synchronized (otherDrones){ otherDrones.wait(time); }
    }

    private void startElectionService(boolean electionMessage, int idElection, int batteryLevelElection) throws InterruptedException {
        ElectionService electionService = new ElectionService(this, electionMessage, idElection, batteryLevelElection);
        electionService.start();
        electionService.join();
    }

    public void startElection() throws InterruptedException {
        synchronized (lockQuit){
            setInElection(true);
            synchronized(lockElectionParticipant){
                Utils.printIfLevel(String.format("Master not available, starting election!"), 0);
                setElectionParticipant(true);
                startElectionService(true, getId(), getUpdatedBatteryLevel());
            }
        }
    }

    public void election(int electionId, int batteryLevelElection) throws InterruptedException {
        setInElection(true);
        broadcastPresentationService.join();
        synchronized(lockElectionParticipant){
            int updatedBatteryLevel = getUpdatedBatteryLevel();
            if(getId() == electionId){
                setElectionParticipant(false);
                startElectionService(false, getId(), 0);
            }else{
                if(updatedBatteryLevel < batteryLevelElection || (updatedBatteryLevel == batteryLevelElection && getId() < electionId)) {
                    setElectionParticipant(true);
                    startElectionService(true, electionId, batteryLevelElection);
                }else{
                    if(updatedBatteryLevel > batteryLevelElection || (updatedBatteryLevel == batteryLevelElection && getId() > electionId)){
                        if(!getElectionParticipant()){
                            setElectionParticipant(true);
                            startElectionService(true, getId(), updatedBatteryLevel);
                        }
                    }
                }
            }
        }
    }

    public void elected(int idElected) throws InterruptedException {
        broadcastPresentationService.join();
        synchronized(lockElectionParticipant){
            if(getId() != idElected){
                setElectionParticipant(false);
                setOtherDroneMaster(idElected);
                startElectionService(false, idElected, 0);
            }else{
                pingService.interrupt();
                setMaster(true);
                setOtherDroneMaster(getId());
                masterService = new MasterService(this);
                masterService.start();
            }
        }
        setInElection(false);
    }

    public Drone nextDrone(){
        Drone ret;
        List<Drone> drones = getOtherDrones();
        drones.add(this);
        drones.sort(Comparator.comparingInt(Drone::getId));
        int i = drones.indexOf(this);
        if(i == (drones.size() - 1))
            ret = drones.get(0);
        else
            ret = drones.get(i + 1);
        return  ret;
    }
    //ELECTION

    public static void main(String[] args){
        try{
            WorkerDrone workerDrone = WorkerDrone.initialize(Config.DRONE_ID_UPPER_BOUND,
                    Config.DRONE_HOSTNAME, Config.ADMINISTRATOR_SERVER_ADDRESS);
            workerDrone.startServicesAndWaitQuit();
        }catch(NotRegisteredDrone notRegisteredDrone){
            System.out.println("REGISTRATION FAILED");
        }catch(Throwable t){
            System.out.println("WORKER DRONE ERROR");
            t.printStackTrace();
        }finally{
            System.out.println("WORKER DRONE TERMINATED");
            System.exit(0);
        }
    }

    //SERVICES
    private void startQuitService(){
        QuitService quitService = new QuitService(this);
        quitService.start();
    }

    private void startDroneService() throws InterruptedException {
        if(droneService == null){
            droneService = new DroneService(this);
            droneService.start();
            droneService.join();
        }
    }

    private void startRegistrationService() throws InterruptedException, NotRegisteredDrone {
        RegistrationService registrationService = new RegistrationService(this);
        registrationService.start();
        registrationService.join();
        if(!registrationService.registered()) throw new NotRegisteredDrone();
    }

    private void startBroadcastPresentationService() throws InterruptedException {
        if(broadcastPresentationService == null && !getMaster()){
            broadcastPresentationService = new BroadcastPresentationService(this);
            broadcastPresentationService.start();
            broadcastPresentationService.join();
        }
    }

    private void startSensorService(){
        if(sensorService == null){
            sensorService = new SensorService(this, new MeasurementBuffer(Config.WINDOW_SIZE, Config.WINDOW_OVERLAP));
            sensorService.start();
        }
    }

    private void startPingService(){
        if(pingService == null && !getMaster()){
            pingService = new PingService(this);
            pingService.start();
        }
    }

    private void startPrintInformationService(){
        if(printInformationService == null){
            printInformationService = new PrintInformationService(this);
            printInformationService.start();
        }
    }

    private void startMasterService(){
        if(masterService == null && getMaster()){
            masterService = new MasterService(this);
            masterService.start();
        }
    }

    public void startServicesAndWaitQuit() throws NotRegisteredDrone, InterruptedException {
        startQuitService();
        startDroneService();
        if(droneService.ok()){
            startRegistrationService();
            startBroadcastPresentationService();
            startSensorService();
            startPingService();
            startPrintInformationService();
            startMasterService();
            waitQuit();
        }
    }

    public void quitDroneService() throws InterruptedException { droneService.shutdownServer(); }

    public void quitSensorService() throws InterruptedException {
        if(sensorService != null){
            sensorService.quitService();
            sensorService.join();
        }
    }

    public void quitPrintInformationService() throws InterruptedException {
        if(printInformationService != null){
            printInformationService.quitService();
            printInformationService.join();
        }
    }

    public void exitFromAdministratorServer(String serverAddress) {
        while(true){
            if(WebServiceClient.getInstance().deleteDrone(serverAddress, getId())){
                break;
            }
        }
    }
    //END SERVICES

}