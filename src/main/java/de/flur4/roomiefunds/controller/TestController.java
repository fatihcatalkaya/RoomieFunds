package de.flur4.roomiefunds.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api/test")
public class TestController {
    @GET
    public String test() {
        return "test";
    }

    @GET
    @Path("/test2")
    public String test2() {
        return "test";
    }
}
