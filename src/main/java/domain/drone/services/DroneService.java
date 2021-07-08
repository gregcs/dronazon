package domain.drone.services;

import domain.drone.base.DroneServiceImpl;
import domain.drone.WorkerDrone;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import utils.Utils;

import java.io.IOException;

public class DroneService extends Thread {

    private final WorkerDrone workerDrone;
    private Server server;
    private final String outputHeader = "DRONE SERVICE:";
    private boolean ok;

    public DroneService(WorkerDrone workerDrone){ this.workerDrone = workerDrone; }

    public void startService() throws IOException {
        server = ServerBuilder.forPort(workerDrone.getPortToDrones()).addService(new DroneServiceImpl(workerDrone)).build();
        server.start();
        Utils.printIfLevel(String.format("%s gRPC SERVER STARTED", outputHeader), 2);
    }

    public void shutdownServer() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination();
        Utils.printIfLevel(String.format("%s gRPC SERVER TERMINATED", outputHeader), 2);
    }

    public boolean ok(){ return ok; }

    @Override
    public void run() {
        try {
            Utils.printIfLevel(String.format("%s STARTED", outputHeader), 2);
            startService();
            ok  = true;
        } catch (Throwable t) {
            Utils.printIfLevel(String.format("%s ERROR", outputHeader), 1);
            Utils.traceIfTest(t);
        }finally{
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader), 2);
        }
    }

}
