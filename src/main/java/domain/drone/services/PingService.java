package domain.drone.services;

import domain.drone.WorkerDrone;
import domain.drone.base.Drone;
import domain.drone.clients.PingClient;
import settings.Config;
import utils.Utils;

import java.util.Random;

public class PingService extends Thread{

    private final WorkerDrone workerDrone;
    private volatile boolean quit;
    private final int waitTime = new Random().nextInt(5001 - 1000) + 1000;
    private final String outputHeader = "PING SERVICE:";

    public PingService(WorkerDrone workerDrone){ this.workerDrone = workerDrone; }

    private void ping() throws InterruptedException {
        while(!quit){
            Drone targetDrone = workerDrone.getOtherDroneMaster();
            if(targetDrone != null){
                PingClient pingClient = new PingClient(workerDrone, targetDrone);
                pingClient.start();
                pingClient.join();
                if(pingClient.getResponse() == null){
                    //Utils.printIfLevel(String.format("%s master %d not available, starting election!", outputHeader, targetDrone.getId()),0 );
                    workerDrone.startElection();
                }
                waitTime();
            }else{
                workerDrone.waitDroneMaster(Config.TIMEOUT_SET_MASTER);
            }
        }
    }

    private synchronized void waitTime() throws InterruptedException {
        wait(waitTime);
    }

    public void quitService() { quit = true; }

    @Override
    public void run() {
        Utils.printIfLevel(String.format("%s START", outputHeader),2);
        try {
            ping();
        }catch(InterruptedException t){
            Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader),1);
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
        }finally{
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}
