package domain.other;

import com.sun.jersey.api.client.*;
import domain.drone.base.Drone;

import rest.beans.responses.DoubleResponse;
import rest.beans.Drones;
import rest.beans.GlobalStatistic;
import rest.beans.responses.GlobalStatisticsResponse;
import utils.Utils;

import javax.ws.rs.core.MediaType;
import java.util.List;

public class WebServiceClient {

    private final Client webClient;
    private static WebServiceClient instance;
    private final String outputHeader = "\nWEBSERVICE CLIENT:";

    private WebServiceClient() { webClient = new Client(); }

    public static synchronized WebServiceClient getInstance(){
        if(instance == null){
            instance = new WebServiceClient();
        }
        return instance;
    }

    public rest.beans.Drones postDrone(String serverAddress, Drone drone) {
        rest.beans.Drones dronesBean = null;
        try {
            ClientResponse response =  webClient.resource(serverAddress + "drones/").type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, Mapper.serializeDroneBean(drone));
            if(response.getStatus() == 304){
                Utils.printIfLevel(String.format("%s A DRONE WITH ID %d ALREADY EXISTS", outputHeader, drone.getId()), 0);
            } else if(response.getStatus() != 200){
                Utils.printIfLevel(String.format("%s RESPONSE ERROR %s", outputHeader, response.getStatus()), 0);
            } else{
                dronesBean = response.getEntity(rest.beans.Drones.class);
            }
        } catch (ClientHandlerException e) {
            Utils.printIfLevel(String.format("%s SERVER NOT AVAILABLE", outputHeader), 0);
        }
        return dronesBean;
    }

    public List<rest.beans.Drone> getDrones(String serverAddress){
        List<rest.beans.Drone> ret = null;
        try {
            WebResource webResourceGet = webClient.resource(serverAddress + "drones");
            ClientResponse response = webResourceGet.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
            if (response.getStatus() != 200) {
                Utils.printIfLevel(String.format("%s RESPONSE ERROR %s", outputHeader, response.getStatus()), 0);
            }else{
                ret = response.getEntity(Drones.class).getDrones();
            }
        } catch (ClientHandlerException e) {
            Utils.printIfLevel(String.format("%s SERVER NOT AVAILABLE", outputHeader), 0);
        }
        return ret;
    }

    public boolean deleteDrone(String baseServerAddress, int droneId){
        boolean ret = false;
        try {
            ClientResponse response =  webClient.resource(baseServerAddress + "drones/" + droneId)
                    .type(MediaType.APPLICATION_JSON_TYPE).delete(ClientResponse.class);
            if(response.getStatus() != 200) {
                Utils.printIfLevel(String.format("%s RESPONSE ERROR %s", outputHeader, response.getStatus()), 0);
            }else{
                Utils.printIfLevel(String.format("%s DRONE %d EXITED SUCCESSFULLY FROM ADMINISTRATOR SERVER", outputHeader, droneId), 0);
                ret = true;
            }
        } catch (ClientHandlerException e) {
            Utils.printIfLevel(String.format("%s SERVER NOT AVAILABLE", outputHeader), 0);
        }
        return ret;
    }

    public boolean postGlobalStatistic(String baseServerAddress, GlobalStatistic globalStatistic){
        boolean ret = false;
        try {
            ClientResponse response =  webClient.resource(baseServerAddress + "global_statistics/").type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, globalStatistic);
            if(response.getStatus() != 200){
                Utils.printIfLevel(String.format("%s RESPONSE ERROR %s", outputHeader, response.getStatus()), 0);
            } else{
                ret = true;
            }
        } catch (ClientHandlerException e) {
            Utils.printIfLevel(String.format("%s SERVER NOT AVAILABLE", outputHeader), 0);
        }
        return ret;
    }

    public List<GlobalStatistic> getLastNGlobalStatistics(String baseServerAddress, int n){
        List<GlobalStatistic> ret = null;
        try {
            ClientResponse response =  webClient.resource(baseServerAddress + "global_statistics/" + n).type(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
            if(response.getStatus() != 200){
                Utils.printIfLevel(String.format("%s RESPONSE ERROR %s", outputHeader, response.getStatus()), 0);
            } else{
                ret = response.getEntity(GlobalStatisticsResponse.class).getGlobalStatisticList();
            }
        } catch (ClientHandlerException e) {
            Utils.printIfLevel(String.format("%s SERVER NOT AVAILABLE", outputHeader), 0);
        }
        return ret;
    }

    public Double getMeanDeliveriesMade(String baseServerAddress, long t1, long t2){
        Double ret = null;
        try {
            ClientResponse response =  webClient.resource(baseServerAddress + String.format("global_statistics/deliveries/%d-%d",t1, t2)).type(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
            if(response.getStatus() != 200){
                Utils.printIfLevel(String.format("%s RESPONSE ERROR %s", outputHeader, response.getStatus()), 0);
            }else{
                ret = response.getEntity(DoubleResponse.class).getValue();
            }
        } catch (ClientHandlerException e) {
            Utils.printIfLevel(String.format("%s SERVER NOT AVAILABLE", outputHeader), 0);
        }
        return ret;
    }

    public Double getMeanKmTraveled(String baseServerAddress, long t1, long t2){
        Double ret = null;
        try {
            ClientResponse response =  webClient.resource(baseServerAddress + String.format("global_statistics/km/%d-%d",t1, t2)).type(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);

            if(response.getStatus() != 200){
                Utils.printIfLevel(String.format("%s RESPONSE ERROR %s", outputHeader, response.getStatus()), 0);
            } else{
                ret = response.getEntity(DoubleResponse.class).getValue();
            }
        } catch (ClientHandlerException e) {
            Utils.printIfLevel(String.format("%s SERVER NOT AVAILABLE", outputHeader), 0);
        }
        return ret;
    }

}
