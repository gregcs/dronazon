package domain.drone.services;

import domain.drone.WorkerDrone;
import domain.drone.base.Drone;
import settings.Config;
import utils.Utils;

public class PrintInformationService extends Thread{

    private final WorkerDrone workerDrone;
    private volatile boolean quit;

    public PrintInformationService(WorkerDrone workerDrone){ this.workerDrone = workerDrone; }

    private void printInformation(){
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("DRONE %d INFORMATION:\n", workerDrone.getId()));
        sb.append(String.format("Deliveries made: %d\n", workerDrone.getDeliveryCounter()));
        sb.append(String.format("Km traveled: %,.2f\n", workerDrone.getKmTraveled()));
        sb.append(String.format("Remaining battery: %d%%", workerDrone.getBatteryLevel()));
        sb.append(String.format("\nNext drone in the ring: %d", workerDrone.nextDrone().getId()));
        String masterId = "NO MASTER, election in progress";
        if(workerDrone.getMaster())
            masterId = Integer.toString(workerDrone.getId());
        else{
            Drone otherMaster = workerDrone.getOtherDroneMaster();
            if(otherMaster != null){
                masterId = Integer.toString(otherMaster.getId());
            }
        }
        sb.append(String.format("\nMaster drone: %s", masterId));
        Utils.printIfLevel(String.format("\n%s\n",sb),0);
    }

    private synchronized void waitTime() throws InterruptedException { wait(Config.TIMEOUT_PRINT_INFORMATION); }

    public void quitService(){ quit = true; }

    @Override
    public void run() {
        String outputHeader = "DRONE PRINT INFORMATION SERVICE:";
        try {
            Utils.printIfLevel(String.format("%s STARTED", outputHeader), 2);
            while(!quit){
                printInformation();
                waitTime();
            }
        }catch(InterruptedException e){
            Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader), 1);
        }catch(Exception e){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
            Utils.traceIfTest(e);
        }finally{
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader), 2);
        }
    }

}
