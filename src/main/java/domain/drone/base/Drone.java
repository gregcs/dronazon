package domain.drone.base;

import domain.position.Position;
import settings.Config;

import java.util.List;

public class Drone {

    private final int id;
    private final String ipAddress;
    private final int portToDrones;
    private Position position;
    private int batteryLevel;
    private boolean master;
    private boolean busy;

    private final Object lockPosition = new Object();
    private final Object lockBatteryLevel = new Object();
    private final Object lockMaster = new Object();
    protected final Object lockBusy = new Object();

    public Drone(int id, String ipAddress, int portToDrones, Position position){
        this.id = id;
        this.ipAddress = ipAddress;
        this.portToDrones = portToDrones;
        this.position = position;
        batteryLevel = Config.DEFAULT_BATTERY_LEVEL;
    }

    public int getId(){ return id; }

    public String getIpAddress(){ return ipAddress; }

    public int getPortToDrones(){ return portToDrones; }

    public Position getPosition(){
        synchronized(lockPosition){
            return position;
        }
    }

    public void setPosition(Position position){
        synchronized(lockPosition){
            this.position = position;
        }
    }

    public void setBatteryLevel(int batteryLevel){
        synchronized(lockBatteryLevel){
            this.batteryLevel = batteryLevel;
        }
    }

    public int getBatteryLevel(){
        synchronized(lockBatteryLevel){
            return batteryLevel;
        }
    }

    public void updateBatteryLevel(){
        synchronized(lockBatteryLevel){
            setBatteryLevel(newBatteryLevel(getBatteryLevel()));
        }
    }

    public int getUpdatedBatteryLevel(){
        synchronized(lockBusy){
            int currentBatteryLevel = getBatteryLevel();
            return getBusy() ? Drone.newBatteryLevel(currentBatteryLevel) : currentBatteryLevel;
        }
    }

    public static int newBatteryLevel(int batteryLevel){
        return  batteryLevel - Config.PERCENTAGE_BATTERY_DECAY;
    }

    public void setMaster(boolean master){
        synchronized(lockMaster){
            this.master = master;
        }
    }

    public boolean getMaster(){
        synchronized(lockMaster){
            return master;
        }
    }

    public void setBusy(boolean busy){
        synchronized(lockBusy){
            this.busy = busy;
        }
    }

    public boolean getBusy(){
        synchronized(lockBusy){
            return busy;
        }
    }

    public void waitIfBusy(long time) throws InterruptedException {
        synchronized(lockBusy){
            if(getBusy())
                lockBusy.wait(time);
        }
    }

    public void setBusyOrWait() throws InterruptedException {
        synchronized(lockBusy){
            if(!getMaster()){
                if(getBusy()){
                    lockBusy.wait();
                }
                setBusy(true);
            }
        }
    }

    public void notifyNotBusy(){
        synchronized(lockBusy){
            setBusy(false);
            lockBusy.notifyAll();
        }
    }

    public static Drone getNearestDroneWithIdBreakingTie(List<Drone> droneList, Position position){
        Drone ret = null;
        double minDistance = Double.MAX_VALUE;
        for (Drone drone: droneList) {
            if(!drone.getBusy()){
                double distance = Position.distance(drone.getPosition(), position);
                double difference = distance - minDistance;
                if(difference < Config.DOUBLE_TOLERANCE){
                    minDistance = distance;
                    ret = drone;
                }else{
                    if(Math.abs(difference) <= Config.DOUBLE_TOLERANCE){
                        if(ret == null){
                            ret = drone;
                        }else{
                            if(drone.getBatteryLevel() > ret.getBatteryLevel()){
                                ret = drone;
                            }else{
                                if(drone.getBatteryLevel() == ret.getBatteryLevel()){
                                    ret = drone.id > ret.id ? drone : ret;
                                }
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        return "Drone{" +
                "id=" + id +
                ", ipAddress='" + ipAddress + '\'' +
                ", portToDrones=" + portToDrones +
                ", position=" + position +
                ", batteryLevel=" + batteryLevel +
                ", master=" + master +
                ", busy=" + busy +
                '}';
    }

}
