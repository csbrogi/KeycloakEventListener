package com.bpanda.keycloak.eventlistener;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.slf4j.event.Level;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BpandaInfluxDBClient {

    private static String serviceName = "keycloak";
    private final InfluxDB influxDB;
    public BpandaInfluxDBClient(String databaseURL, String user, String password) {

        influxDB = InfluxDBFactory.connect(databaseURL, user, password);
    }

    public void write(Level severity, String realm, String clientId, String measurement, String message) {
        BatchPoints batchPoints = BatchPoints
                .database(serviceName)
                .build();

        Point point1 = Point.measurement(measurement)
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("serviceName", serviceName)
                .addField("realm", realm)
                .addField("clientId", clientId)
                .addField("severity", severity.toString())
                .addField("id", UUID.randomUUID().toString())
                .addField("message", message)
                .build();

        try {
            batchPoints.point(point1);
            influxDB.write(batchPoints);
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
