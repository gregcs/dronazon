package rest.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Drone {

    private int id;
    private String ipAddress;
    private int portToDrones;
    private Position position;

    public Drone(){}

    public Drone(int id, String ipAddress, int portToDrones, Position position) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.portToDrones = portToDrones;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) { this.id = id; }

    public String getIpAddress() { return ipAddress; }

    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPortToDrones() { return portToDrones; }

    public void setPortToDrones(int portToDrones) { this.portToDrones = portToDrones; }

    public Position getPosition() { return position; }

    public void setPosition(Position position) { this.position = position; }

    @Override
    public String toString() {
        return "Drone {" +
                "id='" + id + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", portToDrones=" + portToDrones +
                ", position=" + position.toString() +
                '}';
    }
}
