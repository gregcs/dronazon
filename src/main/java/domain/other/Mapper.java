package domain.other;

import domain.drone.WorkerDrone;
import domain.order.Order;
import domain.position.Position;
import domain.drone.base.Drone;
import domain.exceptions.InvalidOrder;
import domain.exceptions.InvalidPosition;
import grpc.domain.DroneServiceOuterClass;
import grpc.domain.OrderOuterClass;
import grpc.domain.PositionOuterClass;

import java.util.ArrayList;
import java.util.List;

public class Mapper {

    public static rest.beans.Drone serializeDroneBean(Drone drone){
        return new rest.beans.Drone(drone.getId(), drone.getIpAddress(), drone.getPortToDrones(), Mapper.serializePositionBean(drone.getPosition()));
    }

    public static List<Drone> deserializeDronesBeanList(List<rest.beans.Drone> dronesBeanList) throws InvalidPosition {
        List<Drone> ret = new ArrayList<>();
        for (rest.beans.Drone droneBean: dronesBeanList) {
            ret.add(new Drone(droneBean.getId(), droneBean.getIpAddress(), droneBean.getPortToDrones(), Mapper.deserializePositionBean(droneBean.getPosition())));
        }
        return ret;
    }

    public static WorkerDrone deserializeDronesBean(WorkerDrone workerDrone, rest.beans.Drones dronesBean) throws InvalidPosition {
        rest.beans.Drone currentDroneBean = dronesBean.getById(workerDrone.getId());
        List<rest.beans.Drone> otherDronesBean = dronesBean.getDrones();
        otherDronesBean.remove(currentDroneBean);
        workerDrone.setPosition(Mapper.deserializePositionBean(currentDroneBean.getPosition()));
        workerDrone.setOtherDrones(Mapper.deserializeDronesBeanList(otherDronesBean));
        workerDrone.setMaster(otherDronesBean.size() == 0);
        return  workerDrone;
    }

    public static Drone deserializePresentationRequest(DroneServiceOuterClass.PresentationRequest presentationRequest) throws InvalidPosition {
        return new Drone(presentationRequest.getId(),
                presentationRequest.getIpAddress(),
                presentationRequest.getPortToDrones(),
                Mapper.deserializePositionMessage(presentationRequest.getPosition())
        );
    }

    public static Position deserializePositionBean(rest.beans.Position positionBean) throws InvalidPosition {
        return new Position(positionBean.getX(), positionBean.getY());
    }

    public static Position deserializePositionMessage(PositionOuterClass.Position position) throws InvalidPosition {
        return new Position(position.getX(), position.getY());
    }

    public static rest.beans.Position serializePositionBean(Position position){
        return new rest.beans.Position(position.x, position.y);
    }

    public static PositionOuterClass.Position serializePositionMessage(Position position) {
        return PositionOuterClass.Position.newBuilder()
                .setX(position.x)
                .setY(position.y)
                .build();
    }

    public static byte[] serializeOrderToByteArray(Order order){
        return OrderOuterClass.Order.newBuilder()
                .setId(order.id.toString())
                .setPickupPosition(PositionOuterClass.Position.newBuilder()
                        .setX(order.pickupPosition.x)
                        .setY(order.pickupPosition.y)
                        .build())
                .setDeliveryPosition(PositionOuterClass.Position.newBuilder()
                        .setX(order.deliveryPosition.x)
                        .setY(order.deliveryPosition.y)
                        .build())
                .build()
                .toByteArray();
    }

    public static Order deserializeOrderMessage(OrderOuterClass.Order order) throws InvalidPosition, InvalidOrder {
        return new Order(order.getId(), Mapper.deserializePositionMessage(order.getPickupPosition()), Mapper.deserializePositionMessage(order.getDeliveryPosition()));
    }

    public static OrderOuterClass.Order serializeOrderMessage(Order order) {
        return OrderOuterClass.Order.newBuilder().setId(order.id.toString())
                .setPickupPosition(Mapper.serializePositionMessage(order.pickupPosition))
                .setDeliveryPosition(Mapper.serializePositionMessage(order.deliveryPosition))
                .build();
    }

}
