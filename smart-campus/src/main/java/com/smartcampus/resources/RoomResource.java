package com.smartcampus.resources;

import com.smartcampus.exceptions.RoomNotEmptyException;
import com.smartcampus.models.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {
    
    private final Map<String, Room> rooms = DataStore.getInstance().getRooms();

    @GET
    public Response getAllRooms() {
        Collection<Room> allRooms = rooms.values();
        return Response.ok(allRooms).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(room).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }
        rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        
        // If room does not exist, return 404 (or optionally 204 or 404 depending on exact design choice, sticking to 404 for clarity here or 204 for absolute idempotency. But 404 is classic).
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Business Logic: Block deletion if sensors are attached
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room " + roomId + " cannot be deleted because it has active sensors assigned to it.");
        }

        rooms.remove(roomId);
        return Response.noContent().build();
    }
}
