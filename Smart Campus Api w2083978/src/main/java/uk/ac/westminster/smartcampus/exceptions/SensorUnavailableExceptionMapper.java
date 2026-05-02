package uk.ac.westminster.smartcampus.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("code", "SENSOR_UNAVAILABLE");
        body.put("sensorId", ex.getSensorId());
        body.put("sensorStatus", ex.getStatus());
        body.put("message", ex.getMessage());
        return Response.status(Response.Status.FORBIDDEN)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
