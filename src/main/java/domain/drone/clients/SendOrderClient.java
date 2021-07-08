package domain.drone.clients;

import domain.drone.base.Drone;
import domain.drone.services.SendOrdersService;
import domain.order.Order;
import domain.other.Mapper;
import domain.exceptions.InvalidPosition;
import settings.Config;

import grpc.domain.DroneServiceGrpc;
import grpc.domain.DroneServiceOuterClass.OrderRequest;
import grpc.domain.DroneServiceOuterClass.OrderResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class SendOrderClient extends Thread{

    private final SendOrdersService sendOrdersService;
    private final Drone targetDrone;
    private final Order orderToSend;
    private OrderResponse response;
    private final String outputHeader = "SEND ORDER CLIENT:";

    public SendOrderClient(SendOrdersService sendOrdersService, Drone targetDrone, Order orderToSend){
        this.sendOrdersService = sendOrdersService;
        this.targetDrone = targetDrone;
        this.orderToSend = orderToSend;
    }

    private OrderRequest buildOrderRequest() {
        return OrderRequest.newBuilder().setId(sendOrdersService.getMasterService().getWorkerDrone().getId())
                .setOrder(Mapper.serializeOrderMessage(orderToSend)).build();
    }

    public void sendOrder() throws InterruptedException {
        String targetAddress = String.format("%s:%s", targetDrone.getIpAddress(), targetDrone.getPortToDrones());
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(targetAddress).usePlaintext().build();
        DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
        OrderRequest orderRequest = buildOrderRequest();
        Utils.printIfLevel(String.format("%s SENDING ORDER %s", outputHeader, orderToSend.id.toString()),2);
        stub.order(orderRequest, new StreamObserver<OrderResponse>() {
            public void onNext(OrderResponse orderResponse){
                response = orderResponse;
                try {
                    sendOrdersService.getMasterService().manageOrderResponse(targetDrone, orderResponse);
                } catch (InvalidPosition invalidPosition) {
                    Utils.printIfLevel(String.format("%s INVALID POSITION", outputHeader),2);
                } catch (InterruptedException e) {
                    Utils.printIfLevel(String.format("%s INTERRUPTED", outputHeader),2);
                }
                Utils.printIfLevel(String.format("%s ORDER %s SENT AND RESPONSE MANAGED", outputHeader, orderToSend.id.toString()),2);
            }
            public void onError(Throwable throwable) {
                Utils.printIfLevel(String.format("%s ERROR| TARGET DRONE %d ERROR -> %s", outputHeader, targetDrone.getId(), throwable.getMessage()),2);
                channel.shutdownNow();
            }
            public void onCompleted() {
                channel.shutdownNow();
            }
        });
        channel.awaitTermination(Config.DEFAULT_GRPC_TIMEOUT, TimeUnit.SECONDS);
        if(response == null){
            sendOrdersService.getMasterService().addOrderIfNotQuitting(orderToSend);
            sendOrdersService.getMasterService().getWorkerDrone().removeOtherDrone(targetDrone);
        }
    }

    @Override
    public void run() {
        try {
            Utils.printIfLevel(String.format("%s STARTED", outputHeader),2);
            sendOrder();
        }catch(Throwable t) {
            Utils.printIfLevel(String.format("%s ERROR", outputHeader),1);
            Utils.traceIfTest(t);
        }finally {
            Utils.printIfLevel(String.format("%s TERMINATED", outputHeader),2);
        }
    }

}
