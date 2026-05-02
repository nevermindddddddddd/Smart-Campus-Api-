package uk.ac.westminster.smartcampus;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.jackson.JacksonFeature;

import uk.ac.westminster.smartcampus.exceptions.GenericExceptionMapper;
import uk.ac.westminster.smartcampus.exceptions.LinkedResourceNotFoundExceptionMapper;
import uk.ac.westminster.smartcampus.exceptions.NotFoundExceptionMapper;
import uk.ac.westminster.smartcampus.exceptions.RoomNotEmptyExceptionMapper;
import uk.ac.westminster.smartcampus.exceptions.SensorUnavailableExceptionMapper;
import uk.ac.westminster.smartcampus.filters.RequestResponseLoggingFilter;
import uk.ac.westminster.smartcampus.resources.DiscoveryResource;
import uk.ac.westminster.smartcampus.resources.SensorResource;
import uk.ac.westminster.smartcampus.resources.SensorRoom;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // JSON provider
        classes.add(JacksonFeature.class);

        // Resources
        classes.add(DiscoveryResource.class);
        classes.add(SensorRoom.class);
        classes.add(SensorResource.class);

        // Exception mappers
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(NotFoundExceptionMapper.class);
        classes.add(GenericExceptionMapper.class);

        // Filters
        classes.add(RequestResponseLoggingFilter.class);

        return classes;
    }
}
