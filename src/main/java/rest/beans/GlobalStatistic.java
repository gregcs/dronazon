package rest.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

@XmlRootElement
public class GlobalStatistic {

    private double averageDeliveries;
    private double averageKmTraveled;
    private double averagePM10;
    private double averageBatteryLevel;
    private long timestamp;

    public GlobalStatistic(){ }

    public double getAverageDeliveries() { return averageDeliveries; }

    public void setAverageDeliveries(double averageDeliveries) { this.averageDeliveries = averageDeliveries; }

    public double getAverageKmTraveled() { return averageKmTraveled; }

    public void setAverageKmTraveled(double averageKmTraveled) { this.averageKmTraveled = averageKmTraveled; }

    public double getAveragePM10() { return averagePM10; }

    public void setAveragePM10(double averagePM10) { this.averagePM10 = averagePM10; }

    public double getAverageBatteryLevel() { return averageBatteryLevel; }

    public void setAverageBatteryLevel(double averageBatteryLevel) { this.averageBatteryLevel = averageBatteryLevel; }

    public long getTimestamp() { return timestamp; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp;}

    @Override
    public String toString() {
        return "GlobalStatistic{ averageDeliveries=" + averageDeliveries +
                String.format(", averageKmTraveled=%,.2f", averageKmTraveled) +
                String.format(", averagePM10=%,.2f", averagePM10) +
                String.format(", averageBatteryLevel=%,.2f", averageBatteryLevel) +
                String.format(", timestamp=%s}", new Timestamp(timestamp)) +
                String.format(", timestamp long=%d}", timestamp);
    }
}
