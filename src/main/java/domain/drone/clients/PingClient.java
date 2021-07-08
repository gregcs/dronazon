package domain.drone.clients;

import domain.drone.WorkerDrone;
import domain.drone.base.Drone;
import grpc.domain.DroneServiceGrpc;
import grpc.domain.DroneServiceOuterClass.PingResponse;
import grpc.domain.DroneServiceOuterClass.PingRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import settings.Config;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class PingClient extends Thread{

    private final WorkerDrone workerDrone;
    private final Drone targetDrone;
    private PingResponse response;

    public PingClient(WorkerDrone workerDrone, Drone targetDrone){
        this.workerDrone = workerDrone;
        this.targetDrone = targetDrone;
    }

    public PingResponse getResponse(){ return response; }

    public PingRequest buildRequest(){
        return PingRequest.newBuilder().setIdFrom(workerDrone.getId()).build();
    }

    public void ping() throws InterruptedException {
        String targetAddress = String.format("%s:%s", targetDrone.getIpAddress(), targetDrone.getPortToDrones());
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(targetAddress).usePlaintext().build();
        DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
        PingRequest pingRequest = buildRequest();
        stub.ping(pingRequest, new StreamObserver<PingResponse>() {
            public void onNext(PingResponse pingResponse) { response = pingResponse; }
            public void onError(Throwable throwable) { channel.shutdownNow(); }
            public void onCompleted() {
                channel.shutdownNow();
            }
        });
        channel.awaitTermination(Config.DEFAULT_GRPC_TIMEOUT, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        String outputHeader = "PING CLIENT:";
        try {
            Utils.printIfLevel(String.format("%s STARTED", outputHeader),2);
            ping();
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
        }finally {
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}
