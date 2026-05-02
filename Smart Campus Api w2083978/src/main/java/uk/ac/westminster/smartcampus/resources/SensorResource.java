package uk.ac.westminster.smartcampus.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import uk.ac.westminster.smartcampus.exceptions.LinkedResourceNotFoundException;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.store.DataStore;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /api/v1/sensors
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public List<Sensor> listSensors(@QueryParam("type") String type) {
        if (type == null || type.isBlank()) {
            return new ArrayList<>(store.sensors().values());
        }
        return store.sensors().values().stream()
                .filter(s -> type.equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{sensorId}")
    public Sensor getSensor(@PathParam("sensorId") String sensorId) {
        Sensor s = store.sensors().get(sensorId);
        if (s == null) throw new NotFoundException("Sensor " + sensorId + " not found");
        return s;
    }

    @POST
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        // Dependency validation: roomId MUST exist.
        Room parent = store.rooms().get(sensor.getRoomId());
        if (parent == null) {
            throw new LinkedResourceNotFoundException("roomId", String.valueOf(sensor.getRoomId()));
        }
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId("SENSOR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }
        store.sensors().put(sensor.getId(), sensor);
        if (!parent.getSensorIds().contains(sensor.getId())) {
            parent.getSensorIds().add(sensor.getId());
        }
        store.readings().putIfAbsent(sensor.getId(), new ArrayList<>());

        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    /**
     * Sub-resource locator: delegates /sensors/{sensorId}/readings to a dedicated class.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        Sensor s = store.sensors().get(sensorId);
        if (s == null) throw new NotFoundException("Sensor " + sensorId + " not found");
        return new SensorReadingResource(sensorId);
    }
}
