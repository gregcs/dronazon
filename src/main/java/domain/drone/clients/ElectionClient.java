package domain.drone.clients;

import domain.drone.WorkerDrone;
import domain.drone.base.Drone;
import grpc.domain.DroneServiceGrpc;
import grpc.domain.DroneServiceOuterClass.ElectionRequest;
import grpc.domain.DroneServiceOuterClass.ElectionResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import settings.Config;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class ElectionClient extends Thread{

    private final WorkerDrone workerDrone;
    private final Drone targetDrone;
    private final int electionId;
    private final int batteryLevelElection;
    private ElectionResponse response;

    private final String outputHeader = "ELECTION CLIENT:";

    public ElectionClient(WorkerDrone workerDrone, Drone targetDrone, int electionId, int batteryLevelElection){
        this.workerDrone = workerDrone;
        this.targetDrone = targetDrone;
        this.electionId = electionId;
        this.batteryLevelElection = batteryLevelElection;
    }

    public ElectionResponse getElectionResponse(){ return response; }

    private ElectionRequest buildElectionRequest(){
        return ElectionRequest.newBuilder()
                .setIdFrom(workerDrone.getId())
                .setIdElection(electionId)
                .setBatteryLevelElection(batteryLevelElection).build();
    }

    public void election() throws InterruptedException{
        String targetAddress = String.format("%s:%s", targetDrone.getIpAddress(), targetDrone.getPortToDrones());
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(targetAddress).usePlaintext().build();
        DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
        stub.election(buildElectionRequest(), new StreamObserver<ElectionResponse>() {
            public void onNext(ElectionResponse electionResponse) { response = electionResponse; }
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
            election();
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
