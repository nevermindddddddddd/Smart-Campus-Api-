package uk.ac.westminster.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.logging.Logger;

/**
 * Bootstraps an embedded Grizzly HTTP server hosting the JAX-RS application.
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://localhost:8080/";

    private Main() {}

    public static void main(String[] args) {
        ResourceConfig config = ResourceConfig.forApplication(new SmartCampusApplication());
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI), config);

        LOG.info("Smart Campus API started at " + BASE_URI + "api/v1");
        LOG.info("Press Ctrl+C to stop the server.");

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
