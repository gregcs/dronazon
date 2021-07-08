package domain.drone.services;

import domain.drone.WorkerDrone;
import domain.drone.base.Drone;
import domain.drone.clients.ElectedClient;
import domain.drone.clients.ElectionClient;
import utils.Utils;

public class ElectionService extends Thread{

    private final WorkerDrone workerDrone;
    private final boolean electionMessage;
    private final int electionId;
    private final int batteryLevelElection;

    public ElectionService(WorkerDrone workerDrone, boolean electionMessage, int electionId, int batteryLevelElection){
        this.workerDrone = workerDrone;
        this.electionMessage = electionMessage;
        this.electionId = electionId;
        this.batteryLevelElection = batteryLevelElection;
    }

    private boolean election(Drone targetDrone) throws InterruptedException {
        ElectionClient electionClient = new ElectionClient(workerDrone, targetDrone, electionId, batteryLevelElection);
        electionClient.start();
        electionClient.join();
        if(electionClient.getElectionResponse() == null){
            workerDrone.removeOtherDrone(targetDrone);
            return false;
        }
        return true;
    }

    private boolean elected(Drone targetDrone) throws InterruptedException {
        ElectedClient electedClient = new ElectedClient(workerDrone, targetDrone, electionId);
        electedClient.start();
        electedClient.join();
        if(electedClient.getElectedResponse() == null){
            workerDrone.removeOtherDrone(targetDrone);
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        String outputHeader = "ELECTION SERVICE:";
        try {
            while(true){
                Drone targetDrone = workerDrone.nextDrone();
                String messageType = electionMessage ? "election" : "elected";
                if(electionMessage){
                    Utils.printIfLevel(String.format("%s sending election message to drone %d with idElected %d and batteryLevel %d",
                            outputHeader, targetDrone.getId(), electionId, batteryLevelElection), 1);
                    if(election(targetDrone))
                        break;
                }else {
                    Utils.printIfLevel(String.format("%s sending elected message to drone %d with idElected %d",
                            outputHeader, targetDrone.getId(), electionId), 1);
                    if(elected(targetDrone))
                        break;
                }
                Utils.printIfLevel(String.format("%s %s message to drone %d failed", outputHeader, messageType, targetDrone.getId()), 1);
            }
        } catch (InterruptedException e) {
            Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader), 1);
        }
    }
}
