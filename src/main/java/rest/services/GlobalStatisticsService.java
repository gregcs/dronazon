package rest.services;
import rest.beans.*;
import rest.beans.responses.DoubleResponse;
import rest.beans.responses.GlobalStatisticsResponse;
import utils.Utils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;

@Path("global_statistics")
public class GlobalStatisticsService {

    @GET
    @Path("{n}")
    @Produces({"application/json"})
    public Response getLastNGlobalStatistics(@PathParam("n") int n){
        Utils.printIfLevel(String.format("Request: last %d global statistics", n),0);
        GlobalStatisticsResponse globalStatisticsResponse = new GlobalStatisticsResponse();
        globalStatisticsResponse.setGlobalStatisticList(GlobalStatistics.getInstance().lastNGlobalStatistics(n));
        return Response.ok(globalStatisticsResponse).build();
    }

    @GET
    @Path("deliveries/{t1}-{t2}")
    @Produces({"application/json"})
    public Response getMeanDeliveriesMade(@PathParam("t1") long t1, @PathParam("t2") long t2){
        Utils.printIfLevel(String.format("Request: average deliveries made between %s and %s", new Timestamp(t1), new Timestamp(t2)),0);
        DoubleResponse doubleResponse = new DoubleResponse();
        doubleResponse.setValue(GlobalStatistics.getInstance().meanDeliveriesMade(t1, t2));
        return Response.ok(doubleResponse).build();
    }

    @GET
    @Path("km/{t1}-{t2}")
    @Produces({"application/json"})
    public Response getMeanKmTraveled(@PathParam("t1") long t1, @PathParam("t2") long t2){
        Utils.printIfLevel(String.format("Request: average Km traveled between %d and %d", t1, t2),0);
        DoubleResponse doubleResponse = new DoubleResponse();
        doubleResponse.setValue(GlobalStatistics.getInstance().meanKmTraveled(t1, t2));
        return Response.ok(doubleResponse).build();
    }

    @POST
    @Produces({"application/json"})
    @Consumes({"application/json"})
    public Response addGlobalStatistic(GlobalStatistic globalStatistic){
        Utils.printIfLevel(String.format("Request: add global statistic %s", globalStatistic),0);
        GlobalStatistics.getInstance().add(globalStatistic);
        Utils.printIfLevel(String.format("Updated global statistics: %s", GlobalStatistics.getInstance().getGlobalStatistics()),0);
        return Response.ok().build();
    }

}
