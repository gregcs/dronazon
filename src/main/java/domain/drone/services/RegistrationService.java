package domain.drone.services;

import domain.drone.base.Drone;
import domain.drone.WorkerDrone;
import domain.exceptions.InvalidPosition;

import domain.other.Mapper;
import domain.other.WebServiceClient;
import utils.Utils;

import java.io.IOException;

public class RegistrationService extends Thread{

    private WorkerDrone workerDrone;
    private final String outputHeader = "REGISTRATION SERVICE:";

    public RegistrationService(WorkerDrone workerDrone){ this.workerDrone = workerDrone; }

    public boolean registered(){ return workerDrone != null; }

    private void register() throws IOException, InvalidPosition {
        rest.beans.Drones dronesBean = WebServiceClient.getInstance()
                    .postDrone(workerDrone.getAdministratorServerAddress(), workerDrone);
        workerDrone = dronesBean != null ? Mapper.deserializeDronesBean(workerDrone, dronesBean) : null;
    }

    private void printRegistrationResult(){
        StringBuilder sb = new StringBuilder();
        sb.append("REGISTRATION DONE!\n");
        if(workerDrone.getMaster())
            sb.append(String.format(" HELLO MASTER DRONE: %s\n", workerDrone));
        else{
            sb.append(String.format(" HELLO DRONE: %s\n", workerDrone));
            sb.append("  LIST OF OTHER DRONES:\n");
            for(Drone d: workerDrone.getOtherDrones()){
                sb.append(String.format("   %s\n", d.toString()));
            }
        }
        System.out.println(sb);
    }

    @Override
    public void run() {
        Utils.printIfLevel(String.format("%s STARTED", outputHeader),2);
        try {
            register();
            if(registered()){
                printRegistrationResult();
            }
        } catch (IOException e) {
            Utils.printIfLevel(String.format("%s ERROR: IO Exception", outputHeader), 2);
        } catch (InvalidPosition invalidPosition) {
            Utils.printIfLevel(String.format("%s ERROR: Invalid position", outputHeader), 2);
        } catch(Exception e){
            Utils.printIfLevel(String.format("%s ERROR: Exception", outputHeader),1);
            Utils.traceIfTest(e);
        } finally{
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader), 2);
        }
    }

}
