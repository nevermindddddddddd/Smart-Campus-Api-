package uk.ac.westminster.smartcampus.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import uk.ac.westminster.smartcampus.exceptions.SensorUnavailableException;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;
import uk.ac.westminster.smartcampus.store.DataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sub-resource for /api/v1/sensors/{sensorId}/readings.
 * Instantiated by SensorResource#readings(...).
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public List<SensorReading> getHistory() {
        return store.readings().getOrDefault(sensorId, new ArrayList<>());
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.sensors().get(sensorId);
        // Existence already validated by the locator, but keep a defensive check.
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())
                || "OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        store.readings()
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        // Side effect: keep parent sensor.currentValue in sync.
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
