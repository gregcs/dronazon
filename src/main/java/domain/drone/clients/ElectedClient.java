package domain.drone.clients;

import domain.drone.WorkerDrone;
import domain.drone.base.Drone;
import grpc.domain.DroneServiceGrpc;
import grpc.domain.DroneServiceOuterClass;
import grpc.domain.DroneServiceOuterClass.ElectedRequest;
import grpc.domain.DroneServiceOuterClass.ElectedResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import settings.Config;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class ElectedClient extends Thread{

    private final WorkerDrone workerDrone;
    private final Drone targetDrone;
    private final int electionId;
    private ElectedResponse response;

    private final String outputHeader = "ELECTED CLIENT:";

    public ElectedClient(WorkerDrone workerDrone, Drone targetDrone, int electionId){
        this.workerDrone = workerDrone;
        this.targetDrone = targetDrone;
        this.electionId = electionId;
    }

    public ElectedResponse getElectedResponse(){ return response; }

    private ElectedRequest buildElectedRequest(){
        return ElectedRequest.newBuilder()
                .setIdFrom(workerDrone.getId()).setIdElected(electionId).build();
    }

    public void elected() throws InterruptedException {
        String targetAddress = String.format("%s:%s", targetDrone.getIpAddress(), targetDrone.getPortToDrones());
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(targetAddress).usePlaintext().build();
        DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
        stub.elected(buildElectedRequest(), new StreamObserver<DroneServiceOuterClass.ElectedResponse>() {
            public void onNext(ElectedResponse electedResponse) {
                response = electedResponse;
            }
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
            elected();
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
