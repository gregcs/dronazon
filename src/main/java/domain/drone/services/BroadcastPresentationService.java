package domain.drone.services;

import domain.drone.base.Drone;
import domain.drone.clients.PresentationClient;
import domain.drone.WorkerDrone;
import grpc.domain.DroneServiceOuterClass.PresentationResponse;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class BroadcastPresentationService extends Thread{

    private final WorkerDrone workerDrone;

    public BroadcastPresentationService(WorkerDrone workerDrone){ this.workerDrone = workerDrone; }

    public void presentation() throws InterruptedException {
        List<PresentationClient> presentationClients = new ArrayList<>();
        for (Drone targetDrone: workerDrone.getOtherDrones()) {
            PresentationClient presentationClient = new PresentationClient(workerDrone, targetDrone);
            presentationClients.add(presentationClient);
            presentationClient.start();
        }
        for (PresentationClient presentationClient : presentationClients) {
            presentationClient.join();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("PRESENTATION ENDED WITH OUTPUT:");
        for (PresentationClient presentationClient : presentationClients) {
            PresentationResponse response = presentationClient.getResponse();
            Drone targetDrone = presentationClient.getTargetDrone();
            sb.append(String.format("\nTarget drone %d response: ", targetDrone.getId()));
            if(response == null){
                sb.append("ERROR");
                workerDrone.removeOtherDrone(targetDrone);
            }else{
                if(response.getMaster()){
                    workerDrone.setOtherDroneMaster(targetDrone.getId());
                    sb.append("MASTER");
                }else{
                    sb.append("OK");
                }
            }
        }
        System.out.println(sb);
    }

    @Override
    public void run() {
        String outputHeader = "BROADCAST PRESENTATION SERVICE:";
        try {
            Utils.printIfLevel(String.format("%s STARTED", outputHeader),2);
            presentation();
        }catch(InterruptedException e) {
            Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader),1);
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
            Utils.traceIfTest(t);
        }finally{
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}
