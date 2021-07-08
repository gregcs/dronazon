package domain.order;

import domain.position.Position;
import domain.exceptions.*;
import settings.Config;
import java.util.Random;
import java.util.UUID;

public class Order {

    public UUID id;
    public Position pickupPosition;
    public Position deliveryPosition;

    public Order() throws InvalidPosition, InvalidOrder {
        Random random = new Random();

        Position pPosition = new Position(random.nextInt(Config.SMART_CITY_DIMENSION),
                random.nextInt(Config.SMART_CITY_DIMENSION));

        Position dPosition = new Position(random.nextInt(Config.SMART_CITY_DIMENSION),
                random.nextInt(Config.SMART_CITY_DIMENSION));

        if(pPosition.equals(dPosition))
            throw new InvalidOrder();

        this.id = UUID.randomUUID();
        this.pickupPosition = pPosition;
        this.deliveryPosition = dPosition;
    }

    public Order(String id, Position pickupPosition, Position deliveryPosition) throws InvalidOrder {
        if(pickupPosition.equals(deliveryPosition))
            throw new InvalidOrder();
        this.id = UUID.fromString(id);
        this.pickupPosition = pickupPosition;
        this.deliveryPosition = deliveryPosition;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", pickupPosition=" + pickupPosition +
                ", deliveryPosition=" + deliveryPosition +
                '}';
    }

}
