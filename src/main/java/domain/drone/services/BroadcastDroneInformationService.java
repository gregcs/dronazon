package domain.drone.services;

import domain.drone.base.Drone;
import domain.drone.clients.DroneInformationClient;
import domain.exceptions.InvalidPosition;
import domain.other.Mapper;
import grpc.domain.DroneServiceOuterClass.InformationResponse;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class BroadcastDroneInformationService extends Thread{

    private final MasterService masterService;

    public BroadcastDroneInformationService(MasterService masterService){ this.masterService = masterService; }

    private void droneInformation() throws InterruptedException, InvalidPosition {
        List<DroneInformationClient> droneInformationClients = new ArrayList<>();
        for(Drone targetDrone: masterService.getWorkerDrone().getOtherDrones()) {
            DroneInformationClient droneInformationClient = new DroneInformationClient(masterService.getWorkerDrone(), targetDrone);
            droneInformationClients.add(droneInformationClient);
            droneInformationClient.start();
        }
        for (DroneInformationClient droneInformationClient : droneInformationClients) {
            droneInformationClient.join();
        }
        for(DroneInformationClient droneInformationClient : droneInformationClients) {
            InformationResponse response = droneInformationClient.getInformationResponse();
            Drone targetDrone = droneInformationClient.getTargetDrone();
            if(response == null){
                masterService.getWorkerDrone().removeOtherDrone(targetDrone);
            }else{
                targetDrone.setPosition(Mapper.deserializePositionMessage(response.getPosition()));
                targetDrone.setBatteryLevel(response.getBatteryLevel());
            }
        }
    }

    @Override
    public void run(){
        String outputHeader = "BROADCAST DRONE INFORMATION SERVICE:";
        try{
            Utils.printIfLevel(String.format("%s STARTED", outputHeader),2);
            droneInformation();
        }catch(InterruptedException e){
            Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader),1);
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
            Utils.traceIfTest(t);
        }finally {
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}
