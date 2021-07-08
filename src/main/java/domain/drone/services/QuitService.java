package domain.drone.services;

import domain.drone.WorkerDrone;
import utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class QuitService extends Thread{

    private final WorkerDrone workerDrone;

    public QuitService(WorkerDrone drone){ this.workerDrone = drone; }

    @Override
    public void run() {
        String outputHeader = "QUIT SERVICE:";
        Utils.printIfLevel(String.format("%s STARTED", outputHeader), 2);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            while(true){
                try {
                    System.out.println("ENTER \"quit\" TO EXIT.");
                    if (br.readLine().equals("quit")) {
                        workerDrone.quit();
                        break;
                    }
                }catch(IOException e){
                    Utils.printIfLevel(String.format("%s IO ERROR", outputHeader), 2);
                }
            }
        }catch(InterruptedException e){
            Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader), 1);
        }finally{
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader), 2);
        }
    }

}
