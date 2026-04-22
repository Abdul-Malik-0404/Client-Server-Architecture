package com.smartcampus.resources;

import com.smartcampus.models.SensorReading;
import com.smartcampus.models.Sensor;
import com.smartcampus.store.DataStore;
import com.smartcampus.exceptions.SensorUnavailableException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final Map<String, List<SensorReading>> sensorReadings = DataStore.getInstance().getSensorReadings();
    private final Map<String, Sensor> sensors = DataStore.getInstance().getSensors();

    // constructor takes sensorId injected by Sub-Resource Locator from SensorResource
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        List<SensorReading> readings = sensorReadings.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(readings).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = sensors.get(sensorId);
        
        // Ensure sensor exists
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        // Check state constraint
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus()) || "OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException("Sensor " + sensorId + " is currently in " + sensor.getStatus() + " and cannot accept new readings.");
        }

        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        sensorReadings.putIfAbsent(sensorId, new ArrayList<>());
        sensorReadings.get(sensorId).add(reading);

        // Side Effect: update current value of parent sensor
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
