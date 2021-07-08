package domain.drone.base;

import domain.drone.WorkerDrone;
import domain.exceptions.InvalidPosition;
import domain.other.Mapper;
import domain.order.Order;

import domain.position.Position;
import io.grpc.stub.StreamObserver;
import grpc.domain.DroneServiceGrpc.*;
import grpc.domain.DroneServiceOuterClass.*;

import utils.Utils;

import java.util.List;

public class DroneServiceImpl extends DroneServiceImplBase {

    private final WorkerDrone workerDrone;
    private final String outputHeader = "DRONE SERVICE -> gRPC SERVER:";

    public DroneServiceImpl(WorkerDrone workerDrone){ this.workerDrone = workerDrone; }

    public static PresentationResponse buildPresentationResponse(boolean ok, boolean master){
        return PresentationResponse.newBuilder()
                .setOk(ok)
                .setMaster(master)
                .build();
    }

    public static InformationResponse buildInformationResponse(int id, Position position, int batteryLevel) throws InvalidPosition {
        return InformationResponse.newBuilder()
                .setId(id)
                .setPosition(Mapper.serializePositionMessage(position))
                .setBatteryLevel(batteryLevel).build();
    }

    public static OrderResponse buildOrderResponse(int droneId, long arrivalTimestamp, Position position,
                                                   double kmTraveled, List<Double> measurementMeansList,
                                                   int batteryLevel) {
        return OrderResponse.newBuilder()
                .setId(droneId)
                .setArrivalTimestamp(arrivalTimestamp)
                .setNewPosition(Mapper.serializePositionMessage(position))
                .setKmTraveled(kmTraveled)
                .addAllMeasurementMeans(measurementMeansList)
                .setBatteryLevel(batteryLevel)
                .build();
    }

    public static PingResponse buildPingResponse(int idFrom){ return PingResponse.newBuilder().setIdFrom(idFrom).build(); }

    public static ElectionResponse buildElectionResponse(int idFrom){ return ElectionResponse.newBuilder().setIdFrom(idFrom).build(); }

    public static ElectedResponse buildElectedResponse(int idFrom){ return ElectedResponse.newBuilder().setIdFrom(idFrom).build(); }

    @Override
    public void presentation(PresentationRequest presentationRequest, StreamObserver<PresentationResponse> responseObserver) {
        try {
            Drone requestDrone = Mapper.deserializePresentationRequest(presentationRequest);
            workerDrone.addOtherDrone(requestDrone);
            Utils.printIfLevel(String.format("%s new drone: %s", outputHeader, requestDrone),1);
            responseObserver.onNext(buildPresentationResponse(true, workerDrone.getMaster()));
            responseObserver.onCompleted();
        } catch (InvalidPosition invalidPosition) {
            Utils.printIfLevel(String.format("%s INVALID POSITION", outputHeader), 2);
        }
    }

    @Override
    public void droneInformation(InformationRequest informationRequest, StreamObserver<InformationResponse> responseObserver) {
        try {
            Utils.printIfLevel(String.format("%s master node %d has requested drone information.", outputHeader, informationRequest.getId()),1);
            responseObserver.onNext(buildInformationResponse(workerDrone.getId(), workerDrone.getPosition(), workerDrone.getBatteryLevel()));
            responseObserver.onCompleted();
        } catch (InvalidPosition invalidPosition) {
            Utils.printIfLevel(String.format("%s INVALID POSITION", outputHeader), 2);
        }
    }

    @Override
    public void order(OrderRequest orderRequest, StreamObserver<OrderResponse> responseObserver) {
        try {
            workerDrone.orderArrived();
            if(!workerDrone.lowBattery() && !workerDrone.getQuit()){
                if(!workerDrone.getMaster() && workerDrone.getOtherDroneMaster() == null){
                    workerDrone.setOtherDroneMaster(orderRequest.getId());
                }
                Order order = Mapper.deserializeOrderMessage(orderRequest.getOrder());
                Utils.printIfLevel(String.format("%s order received from master %d: %s", outputHeader, orderRequest.getId(), order),1);
                OrderResponse orderResponse = workerDrone.delivery(order);
                responseObserver.onNext(orderResponse);
                responseObserver.onCompleted();
            }
        }catch(Throwable t){
            Utils.printIfLevel(String.format("%s ORDER ERROR", outputHeader), 1);
            Utils.traceIfTest(t);
        }finally{
            try {
                workerDrone.orderCompleted();
            } catch (InterruptedException e) {
                Utils.traceIfTest(e);
            }
        }
    }

    @Override
    public void election(ElectionRequest electionRequest, StreamObserver<ElectionResponse> responseObserver){
        try {
            Utils.printIfLevel(String.format("%s election message received from drone %d, electing drone %d with battery %d%%",
                    outputHeader, electionRequest.getIdFrom(), electionRequest.getIdElection(), electionRequest.getBatteryLevelElection()), 1);
            responseObserver.onNext(buildElectionResponse(workerDrone.getId()));
            responseObserver.onCompleted();
            workerDrone.election(electionRequest.getIdElection(), electionRequest.getBatteryLevelElection());
        }catch(InterruptedException e){
            Utils.printIfLevel("Message election interrupted",2);
        }
    }

    @Override
    public void elected(ElectedRequest electedRequest, StreamObserver<ElectedResponse> responseObserver) {
        try {
            Utils.printIfLevel(String.format("%s elected message received from drone %d, elected drone %d",
                    outputHeader, electedRequest.getIdFrom(), electedRequest.getIdElected()), 1);
            responseObserver.onNext(buildElectedResponse(workerDrone.getId()));
            responseObserver.onCompleted();
            workerDrone.elected(electedRequest.getIdElected());
        }catch(InterruptedException e){
            Utils.printIfLevel("Message elected interrupted",2);
        }
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        responseObserver.onNext(buildPingResponse(workerDrone.getId()));
        responseObserver.onCompleted();
    }

}