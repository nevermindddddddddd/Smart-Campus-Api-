package uk.ac.westminster.smartcampus.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

/**
 * Cross-cutting concern: logs every inbound request and outbound response.
 * Avoids polluting every resource method with Logger.info(...) calls.
 */
@Provider
public class RequestResponseLoggingFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger("SmartCampusAPI");

    @Override
    public void filter(ContainerRequestContext requestContext) {
        LOG.info("--> " + requestContext.getMethod()
                + " " + requestContext.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        LOG.info("<-- " + requestContext.getMethod()
                + " " + requestContext.getUriInfo().getRequestUri()
                + " :: " + responseContext.getStatus());
    }
}
