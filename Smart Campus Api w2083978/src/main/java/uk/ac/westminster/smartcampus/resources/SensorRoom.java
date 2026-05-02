package uk.ac.westminster.smartcampus.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import uk.ac.westminster.smartcampus.exceptions.RoomNotEmptyException;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.store.DataStore;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

/**
 * /api/v1/rooms
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoom {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Collection<Room> listRooms() {
        return store.rooms().values();
    }

    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        if (room.getId() == null || room.getId().isBlank()) {
            room.setId("ROOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        store.rooms().put(room.getId(), room);

        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Room getRoom(@PathParam("roomId") String roomId) {
        Room room = store.rooms().get(roomId);
        if (room == null) {
            throw new NotFoundException("Room " + roomId + " not found");
        }
        return room;
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.rooms().get(roomId);
        if (room == null) {
            // Idempotency-friendly: deleting a missing room returns 404 once.
            throw new NotFoundException("Room " + roomId + " not found");
        }
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }
        store.rooms().remove(roomId);
        return Response.noContent().build();
    }
}
