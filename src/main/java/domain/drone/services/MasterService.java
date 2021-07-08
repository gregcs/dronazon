package domain.drone.services;

import domain.drone.base.Drone;
import domain.drone.base.DroneStatistic;
import domain.drone.WorkerDrone;
import domain.exceptions.InvalidPosition;
import domain.order.Order;
import domain.other.Mapper;
import domain.other.Queue;
import domain.position.Position;
import grpc.domain.DroneServiceOuterClass;
import rest.beans.GlobalStatistic;
import settings.Config;
import utils.Utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class MasterService extends Thread{

    private final WorkerDrone workerDrone;
    private final Queue<Order> orderQueue = new Queue<>();
    private final Map<Integer, DroneStatistic> droneStatistics = new HashMap<>();

    private final Object lockChooseDrone = new Object();

    private boolean quit;
    private final Object lockQuit = new Object();

    private OrderReceiverService orderReceiverService;
    private SendOrdersService sendOrdersService;
    private SendGlobalStatisticsService sendGlobalStatisticsService;

    private final String outputHeader = "MASTER SERVICE:";

    public MasterService(WorkerDrone workerDrone){
        this.workerDrone = workerDrone;
    }

    public WorkerDrone getWorkerDrone(){ return workerDrone; }

    //ORDER
    //called by OrderReceiverService's callback
    public void addOrder(Order order){ orderQueue.put(order); }

    //called by SendOrdersService and SendOrderClient's callback
    public void addOrderIfNotQuitting(Order order){
        synchronized(lockQuit){
            if(!getQuit()){
                addOrder(order);
            }
        }
    }

    //called by SendOrdersService
    public Order takeOrderAndNotifyIfEmpty() { return orderQueue.takeAndNotifyIfEmpty(); }

    //called by SendOrdersService
    public Drone chooseAndBlockDrone(Order order){
        synchronized(lockChooseDrone){
            List<Drone> dronesList = workerDrone.getOtherDrones();
            dronesList.add(workerDrone);
            Drone chosenDrone = Drone.getNearestDroneWithIdBreakingTie(dronesList, order.pickupPosition);
            if(chosenDrone != null)
                chosenDrone.setBusy(true);
            return chosenDrone;
        }
    }

    public void orderCompleted() throws InterruptedException {
        synchronized (lockChooseDrone){
            workerDrone.notifyNotBusy();
            workerDrone.lowBatteryQuit();
        }
    }

    //called by SendOrderClient's callback
    public void releaseAndUpdateDrone(Drone drone, int batteryLevel, Position position) throws InterruptedException {
        if(!drone.getMaster()){
            synchronized(lockChooseDrone){
                if(batteryLevel <= Config.PERCENTAGE_BATTERY_LIMIT_TO_QUIT){
                    workerDrone.removeOtherDrone(drone);
                }else{
                    if(batteryLevel > Config.PERCENTAGE_BATTERY_LIMIT_TO_QUIT){
                        drone.setBatteryLevel(batteryLevel);
                        drone.setPosition(position);
                    }
                    drone.setBusy(false);
                }
            }
        }else{
            orderCompleted();
        }
    }

    //called by SendOrderClient's callback
    public void manageOrderResponse(Drone drone, DroneServiceOuterClass.OrderResponse orderResponse) throws InvalidPosition, InterruptedException {
        Utils.printIfLevel(String.format("%s Order response received from drone %d, order delivered at time: %s",
                outputHeader, orderResponse.getId(), new Timestamp(orderResponse.getArrivalTimestamp())), 1);
        updateDroneStatistics(drone.getId(), orderResponse.getKmTraveled(), orderResponse.getMeasurementMeansList(), orderResponse.getBatteryLevel());
        releaseAndUpdateDrone(drone, orderResponse.getBatteryLevel(), Mapper.deserializePositionMessage(orderResponse.getNewPosition()));
    }
    //END ORDER

    //STATISTICS
    private void updateDroneStatistics(int droneId, double kmTraveled, List<Double> measurementMeansList, double batteryLevel){
        synchronized(droneStatistics){
            if(droneStatistics.get(droneId) != null){
                droneStatistics.get(droneId).updateStatistic(kmTraveled, measurementMeansList, batteryLevel);
            }else{
                DroneStatistic droneStatistic = new DroneStatistic();
                droneStatistic.updateStatistic(kmTraveled, measurementMeansList, batteryLevel);
                droneStatistics.put(droneId, droneStatistic);
            }
        }
    }

    private List<DroneStatistic> getDroneStatisticsAndClear(){
        synchronized(droneStatistics){
            List<DroneStatistic> ret = new ArrayList<>(new HashMap<>(droneStatistics).values());
            droneStatistics.clear();
            return ret;
        }
    }

    public GlobalStatistic getGlobalStatistic(){
        GlobalStatistic globalStatistic =  null;
        List<DroneStatistic> droneStatistics = getDroneStatisticsAndClear();
        if(!droneStatistics.isEmpty()){
            globalStatistic = new GlobalStatistic();
            int dronesCount = droneStatistics.size();
            int measurementCount = 0;
            int sumDeliveries = 0;
            double sumKm = 0;
            double sumPM10 = 0;
            double sumBatteryLevel = 0;
            for (DroneStatistic droneStatistic: droneStatistics) {
                sumDeliveries +=  droneStatistic.deliveryCount;
                sumKm += droneStatistic.kmTraveled;
                measurementCount += droneStatistic.measurementMeans.size();
                for(Double measurement: droneStatistic.measurementMeans) {
                    sumPM10 += measurement;
                }
                sumBatteryLevel += droneStatistic.batteryLevel;
            }
            globalStatistic.setAverageDeliveries((float)sumDeliveries/dronesCount);
            globalStatistic.setAverageKmTraveled(sumKm/dronesCount);
            globalStatistic.setAveragePM10(sumPM10/measurementCount);
            globalStatistic.setAverageBatteryLevel(sumBatteryLevel/dronesCount);
            globalStatistic.setTimestamp(new Timestamp(System.currentTimeMillis()).getTime());
        }
        return globalStatistic;
    }
    //END STATISTICS

    //QUIT
    public void setQuit() {
        synchronized(lockQuit){
            quit = true;
        }
    }

    public boolean getQuit() {
        synchronized (lockQuit) {
            return quit;
        }
    }

    //called from WorkerDrone quit method with WorkerDrone's lockQuit already taken
    public void quit() throws InterruptedException {
        workerDrone.quitPrintInformationService();
        workerDrone.waitToCompleteOrder(Config.DEFAULT_GRPC_TIMEOUT);
        orderReceiverService.disconnect();
        setQuit();
        orderQueue.waitIfNotEmptyQueue();
        sendOrdersService.waitToReceiveAllOrdersResponses();
        workerDrone.waitElectionAndQuitDroneService();
        quitSendGlobalStatisticsService();
        workerDrone.exitFromAdministratorServer(Config.ADMINISTRATOR_SERVER_ADDRESS);
        workerDrone.quitSensorService();
        workerDrone.setQuitAndNotify();
    }
    //END QUIT

    //SERVICES
    private void startBroadcastDroneInformationService() throws InterruptedException {
        BroadcastDroneInformationService broadcastDroneInformationService = new BroadcastDroneInformationService(this);
        broadcastDroneInformationService.start();
        broadcastDroneInformationService.join();
    }

    private void startOrderReceiverService(){
        if(orderReceiverService == null){
            orderReceiverService = new OrderReceiverService(this);
            orderReceiverService.start();
        }
    }

    private void startSendOrderService(){
        if(sendOrdersService == null){
            sendOrdersService = new SendOrdersService(this);
            sendOrdersService.start();
        }
    }

    private void startSendGlobalStatisticService(){
        if(sendGlobalStatisticsService == null){
            sendGlobalStatisticsService = new SendGlobalStatisticsService(this);
            sendGlobalStatisticsService.start();
        }
    }

    private void startServices() throws InterruptedException {
        startBroadcastDroneInformationService();
        startOrderReceiverService();
        startSendOrderService();
        startSendGlobalStatisticService();
    }

    private void quitSendGlobalStatisticsService() throws InterruptedException {
        sendGlobalStatisticsService.quitService();
        sendGlobalStatisticsService.join();
    }
    //END SERVICES

    @Override
    public void run(){
        try {
            Utils.printIfLevel(String.format("%s STARTED", outputHeader),2);
            startServices();
        }catch(InterruptedException t){
            Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader), 1);
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader), 1);
            Utils.traceIfTest(t);
        }finally{
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}