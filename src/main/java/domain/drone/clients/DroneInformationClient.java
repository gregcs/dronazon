package domain.drone.clients;

import domain.drone.WorkerDrone;
import domain.drone.base.Drone;
import grpc.domain.DroneServiceGrpc;
import grpc.domain.DroneServiceOuterClass.InformationRequest;
import grpc.domain.DroneServiceOuterClass.InformationResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import settings.Config;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class DroneInformationClient extends Thread{

    private final WorkerDrone workerDrone;
    private final Drone targetDrone;
    private InformationResponse response;

    private final String outputHeader = "DRONE INFORMATION CLIENT:";

    public DroneInformationClient(WorkerDrone workerDrone, Drone targetDrone){
        this.workerDrone = workerDrone;
        this.targetDrone = targetDrone;
    }

    public InformationResponse getInformationResponse(){ return response; }

    public Drone getTargetDrone(){ return targetDrone; }

    private InformationRequest buildPositionRequest() {
        return InformationRequest.newBuilder().setId(workerDrone.getId()).build();
    }

    public void droneInformation() throws InterruptedException {
        String targetAddress = String.format("%s:%s", targetDrone.getIpAddress(), targetDrone.getPortToDrones());
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(targetAddress).usePlaintext().build();
        DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
        InformationRequest informationRequest = buildPositionRequest();
        stub.droneInformation(informationRequest, new StreamObserver<InformationResponse>() {
            public void onNext(InformationResponse informationResponse) { response = informationResponse; }
            public void onError(Throwable throwable) {
                Utils.printIfLevel(String.format("%s ERROR| TARGET %d | ERROR -> %s\n",
                        outputHeader, targetDrone.getId(), throwable.getMessage()),1);
                channel.shutdownNow();
            }
            public void onCompleted() { channel.shutdownNow(); }
        });
        channel.awaitTermination(Config.DEFAULT_GRPC_TIMEOUT, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            Utils.printIfLevel(String.format("%s STARTED", outputHeader),2);
            droneInformation();
        }catch (InterruptedException e) {
            Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader),1);
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
        }
        finally {
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}
