package com.smartcampus.resources;

import com.smartcampus.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final Map<String, Sensor> sensors = DataStore.getInstance().getSensors();
    private final Map<String, Room> rooms = DataStore.getInstance().getRooms();

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = sensors.values().stream()
                .filter(s -> type == null || type.equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());
        return Response.ok(result).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(sensor).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        // Dependency Validation: Ensure roomId exists
        String roomId = sensor.getRoomId();
        if (roomId == null || !rooms.containsKey(roomId)) {
            throw new LinkedResourceNotFoundException("Room ID " + roomId + " does not exist.");
        }

        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }
        
        sensors.put(sensor.getId(), sensor);

        // Update room's sensor list
        Room room = rooms.get(roomId);
        if (room != null) {
            room.getSensorIds().add(sensor.getId());
        }

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // Sub-Resource Locator
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        // Delegate to the Sub-Resource
        return new SensorReadingResource(sensorId);
    }
}
