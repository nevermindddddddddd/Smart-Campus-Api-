package uk.ac.westminster.smartcampus.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "safety net" mapper. Intercepts ANY uncaught Throwable and returns
 * a sanitized JSON 500 response instead of a raw stack trace, which would be
 * a security risk (information disclosure).
 *
 * WebApplicationException subclasses are re-thrown so JAX-RS can use their
 * own status (or our specific mappers above).
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof WebApplicationException) {
            return ((WebApplicationException) ex).getResponse();
        }

        String correlationId = UUID.randomUUID().toString();
        // Log the full trace SERVER-SIDE only.
        LOG.log(Level.SEVERE, "Unhandled exception [" + correlationId + "]", ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("code", "INTERNAL_ERROR");
        body.put("message", "An unexpected error occurred. Please contact support.");
        body.put("correlationId", correlationId);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
