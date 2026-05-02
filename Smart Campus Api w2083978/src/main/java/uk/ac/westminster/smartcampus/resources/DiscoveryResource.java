package uk.ac.westminster.smartcampus.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /api/v1   - HATEOAS-style discovery document.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Smart Campus Sensor & Room Management API");
        body.put("version", "1.0.0");
        body.put("apiVersion", "v1");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("administrator", "Smart Campus Operations");
        contact.put("email", "smartcampus-admin@westminster.ac.uk");
        body.put("contact", contact);

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        links.put("readings (per sensor)", "/api/v1/sensors/{sensorId}/readings");
        body.put("_links", links);

        return Response.ok(body).build();
    }
}
