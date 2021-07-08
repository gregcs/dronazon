package rest.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Drones {

    private List<Drone> drones;

    private static Drones instance;

    private Drones() { drones = new ArrayList<>(); }

    public static synchronized Drones getInstance(){
        if(instance==null)
            instance = new Drones();
        return instance;
    }

    public synchronized List<Drone> getDrones() { return new ArrayList<>(drones); }

    public synchronized void setDrones(List<Drone> drones) { this.drones = drones; }

    public synchronized Drone add(Drone d){
        Drone ret = getById(d.getId());
        if(ret == null)
            drones.add(d);
        return ret;
    }

    public synchronized Drone delete(int id){
        Drone ret = getById(id);
        if(ret != null)
            drones.remove(ret);
        return ret;
    }

    public Drone getById(int id){
        Drone ret = null;
        List<Drone> dronesCopy = getDrones();
        for(Drone d: dronesCopy)
            if(d.getId() == id){
                ret = d;
                break;
            }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(Drone d: getDrones()){
            builder.append(d.toString());
            builder.append("\n");
        }
        return builder.toString();
    }

}
