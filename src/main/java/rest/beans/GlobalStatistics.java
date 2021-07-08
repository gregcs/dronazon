package rest.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GlobalStatistics {

    private List<GlobalStatistic> globalStatistics;

    private static GlobalStatistics instance;

    private GlobalStatistics() { globalStatistics = new ArrayList<>(); }

    public static synchronized GlobalStatistics getInstance(){
        if(instance==null)
            instance = new GlobalStatistics();
        return instance;
    }

    public synchronized List<GlobalStatistic> getGlobalStatistics() { return new ArrayList<>(globalStatistics); }

    public synchronized void setGlobalStatistics(List<GlobalStatistic> globalStatistics) { this.globalStatistics = globalStatistics; }

    public synchronized void add(GlobalStatistic globalStatistic){ globalStatistics.add(globalStatistic); }

    public List<GlobalStatistic> lastNGlobalStatistics(int n){
        List<GlobalStatistic> globalStatistics = getGlobalStatistics();
        if(globalStatistics.size() > n){
            globalStatistics.removeAll(globalStatistics.subList(0, globalStatistics.size() - n));
        }
       return globalStatistics;
    }

    public Double meanDeliveriesMade(long t1, long t2){
        List<GlobalStatistic> globalStatistics = getGlobalStatistics();
        double sum = 0;
        int count = 0;
        for (GlobalStatistic globalStatistic:globalStatistics) {
            if(globalStatistic.getTimestamp() >= t1 && globalStatistic.getTimestamp() <= t2){
                sum += globalStatistic.getAverageDeliveries();
                count += 1;
            }
        }
        return sum/count;
    }

    public Double meanKmTraveled(long t1, long t2){
        List<GlobalStatistic> globalStatistics = getGlobalStatistics();
        double sum = 0;
        int count = 0;
        for (GlobalStatistic globalStatistic:globalStatistics) {
            if(globalStatistic.getTimestamp() >= t1 && globalStatistic.getTimestamp() <= t2){
                sum += globalStatistic.getAverageKmTraveled();
                count += 1;
            }
        }
        return sum/count;
    }

}
