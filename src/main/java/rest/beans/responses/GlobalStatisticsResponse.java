package rest.beans.responses;

import rest.beans.GlobalStatistic;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GlobalStatisticsResponse {

    private List<GlobalStatistic> globalStatisticList;

    public GlobalStatisticsResponse(){ globalStatisticList = new ArrayList<>(); }

    public List<GlobalStatistic> getGlobalStatisticList() { return globalStatisticList; }

    public void setGlobalStatisticList(List<GlobalStatistic> globalStatisticList) { this.globalStatisticList = globalStatisticList; }

}
