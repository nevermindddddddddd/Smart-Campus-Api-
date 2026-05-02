package uk.ac.westminster.smartcampus.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 409);
        body.put("error", "Conflict");
        body.put("code", "ROOM_NOT_EMPTY");
        body.put("message", ex.getMessage());
        body.put("roomId", ex.getRoomId());
        body.put("sensorCount", ex.getSensorCount());
        return Response.status(Response.Status.CONFLICT)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
