package com.bpanda.keycloak.eventlistener;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.keycloak.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BpandaInfluxDBClient {
    private static final Logger log = LoggerFactory.getLogger(BpandaInfluxDBClient.class);

    private final InfluxDB influxDB;
    private final String influxDBName;
    private final String influxdbDBServiceName;
    private final String influxDBRetention;

    public static BpandaInfluxDBClient createBpandaInfluxDBClient(String databaseURL, String user, String password, String influxDBName, String influxdbDBServiceName, String influxDBRetention) {
        BpandaInfluxDBClient ret = null;
        try {
            ret = new BpandaInfluxDBClient(databaseURL, user, password, influxDBName, influxdbDBServiceName, influxDBRetention);
        } catch (Exception e) {
            log.error("cannot connect to influx db:", e);
        }
        return ret;
    }
    private BpandaInfluxDBClient(String databaseURL, String user, String password, String influxDBName, String influxdbDBServiceName, String influxDBRetention) {
        this.influxDBName = influxDBName;
        this.influxDBRetention = influxDBRetention;
        this.influxdbDBServiceName = influxdbDBServiceName;
        influxDB = InfluxDBFactory.connect(databaseURL, user, password);
        if (influxDBRetention != null && !influxDBRetention.isEmpty()) {
            influxDB.createRetentionPolicy(influxDBRetention, influxDBName, influxDBRetention, null, 1);
        }
    }


    public  void logError(Event event){
        String error = String.format("%s - Realm: %s ClientId: %s", event.getType().toString(), event.getRealmId(), event.getClientId());
        String severity = "ERROR";
        if ("expired_code".equals(event.getError())) {
            severity = "WARN";
        }
        StringBuilder cause = new StringBuilder(event.getError()).append( ": ");
        Map<String, String> details = event.getDetails();
        if (null != details && !details.isEmpty()) {
            cause.append(details.entrySet().stream()
                    .map(e-> e.getKey()+": "+e.getValue())
                    .collect(Collectors.joining(", ")));
        }
        Point.Builder pb = Point.measurement("incident").
                tag("serviceName", this.influxdbDBServiceName).
                tag("severity", severity).
                tag("client", event.getClientId()).
                tag("realm", event.getRealmId()).
                addField("id", event.getId()).
                addField("message", error).
                addField("cause", cause.toString()).
                time(event.getTime(), TimeUnit.MILLISECONDS);
        try {
            influxDB.write(influxDBName, influxDBRetention, pb.build());
        } catch (Exception e) {
            log.error("cannot write message to influx db:", e);
        }
    }

    public void logInfo(String eventId, String category, String operation, long eventTime, String realm, String clientId) {
        String severity = "INFO";
        Point.Builder pb = Point.measurement("kc-events").
                tag("serviceName", this.influxdbDBServiceName).
                tag("category", category).
                tag("severity", severity).
                addField("id", eventId).
                time(eventTime, TimeUnit.MILLISECONDS);

        if (null != realm) {
            pb.tag("realm", realm);
        }
        if (null != clientId) {
            pb.tag("client", clientId);
        }
        if (null != operation) {
            pb.tag("operation", operation);
        }
        try {
            influxDB.write(influxDBName, influxDBRetention, pb.build());
        } catch (Exception e) {
            log.error("cannot write info to influx db:", e);
        }
    }

    public void close() {
        influxDB.close();
    }
}
