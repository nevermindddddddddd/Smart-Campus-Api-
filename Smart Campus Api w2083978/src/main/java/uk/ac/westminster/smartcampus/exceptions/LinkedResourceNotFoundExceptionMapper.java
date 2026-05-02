package uk.ac.westminster.smartcampus.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    // 422 Unprocessable Entity (per RFC 4918)
    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", UNPROCESSABLE_ENTITY);
        body.put("error", "Unprocessable Entity");
        body.put("code", "LINKED_RESOURCE_NOT_FOUND");
        body.put("field", ex.getFieldName());
        body.put("missingId", ex.getMissingId());
        body.put("message", ex.getMessage());
        return Response.status(UNPROCESSABLE_ENTITY)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
