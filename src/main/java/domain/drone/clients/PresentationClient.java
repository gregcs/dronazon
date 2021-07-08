package domain.drone.clients;

import domain.drone.base.Drone;
import domain.drone.WorkerDrone;
import domain.other.Mapper;

import grpc.domain.DroneServiceGrpc;
import grpc.domain.DroneServiceOuterClass.PresentationResponse;
import grpc.domain.DroneServiceOuterClass.PresentationRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import settings.Config;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class PresentationClient extends Thread {

    private final WorkerDrone workerDrone;
    private final Drone targetDrone;
    private PresentationResponse response;

    private final String outputHeader = "PRESENTATION CLIENT:";

    public PresentationClient(WorkerDrone workerDrone, Drone targetDrone){
        this.workerDrone = workerDrone;
        this.targetDrone = targetDrone;
    }

    public PresentationResponse getResponse(){ return response; }

    public Drone getTargetDrone(){ return targetDrone; }

    public PresentationRequest buildRequest() {
        return PresentationRequest.newBuilder()
                .setId(workerDrone.getId())
                .setIpAddress(workerDrone.getIpAddress())
                .setPortToDrones(workerDrone.getPortToDrones())
                .setPosition(Mapper.serializePositionMessage(workerDrone.getPosition()))
                .build();
    }

    public void presentation() throws InterruptedException {
        String targetAddress = String.format("%s:%s", targetDrone.getIpAddress(), targetDrone.getPortToDrones());
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(targetAddress).usePlaintext().build();
        DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
        PresentationRequest presentationRequest = buildRequest();
        stub.presentation(presentationRequest, new StreamObserver<PresentationResponse>() {
            public void onNext(PresentationResponse presentationResponse) { response = presentationResponse; }
            public void onError(Throwable throwable) {
                Utils.printIfLevel(String.format("%s ERROR| TARGET %d | ERROR -> %s",
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
            presentation();
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
            Utils.traceIfTest(t);
        }finally {
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}