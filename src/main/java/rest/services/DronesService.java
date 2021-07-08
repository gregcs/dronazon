package rest.services;

import rest.beans.*;
import utils.Utils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("drones")
public class DronesService {

    @GET
    @Produces({"application/json"})
    public Response getDronesList(){
        Utils.printIfLevel("Request: drones",0);
        return Response.ok(Drones.getInstance()).build();
    }

    @POST
    @Produces({"application/json"})
    @Consumes({"application/json"})
    public Response addDrone(Drone drone){
        Utils.printIfLevel(String.format("Request: add drone %s", drone),0);
        drone.setPosition(Position.generateRandomPosition());
        Drone added = Drones.getInstance().add(drone);
        Utils.printIfLevel(String.format("Updated drones: %s", Drones.getInstance().getDrones()),0);
        return added == null ? Response.ok(Drones.getInstance()).build() : Response.notModified().build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteDrone(@PathParam("id") int id) {
        Utils.printIfLevel(String.format("Request: delete drone %d", id),0);
        Drone deleted = Drones.getInstance().delete(id);
        Utils.printIfLevel(String.format("Updated drones: %s", Drones.getInstance().getDrones()),0);
        return deleted == null ? Response.noContent().build() : Response.ok().build();
    }

}
