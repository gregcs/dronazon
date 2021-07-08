package domain.drone.base;

import java.util.ArrayList;
import java.util.List;

public class DroneStatistic{

    public int deliveryCount;
    public double kmTraveled;
    public List<Double> measurementMeans = new ArrayList<>();
    public double batteryLevel;

    public void updateStatistic(double kmDelivered, List<Double> measurementMeans, double batteryLevel){
        deliveryCount += 1;
        this.kmTraveled += kmDelivered;
        this.measurementMeans.addAll(measurementMeans);
        this.batteryLevel = batteryLevel;
    }

}