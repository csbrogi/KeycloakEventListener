package com.bpanda.keycloak.eventlistener;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.RealmModel;
import org.keycloak.timer.TimerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.stream.Collectors;

public class BpandaEventListenerProviderFactory implements EventListenerProviderFactory {
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProviderFactory.class);
    private KafkaProducer producer;

    private KafkaAdapter adapter;

    private BpandaInfluxDBClient bpandaInfluxDBClient;

    private long updateTime = 12; // Update timer in seconds
    private long counter = 0;

    private static final String KAFKA_HOST = "KAFKA_HOST";
    private static final String KAFKA_PORT = "KAFKA_PORT";

    private String identityHost;
    private String identityPort;

    @Override
    public EventListenerProvider create(KeycloakSession aKeycloakSession) {
        return new BpandaEventListenerProvider(this.identityHost, identityPort, producer, bpandaInfluxDBClient, aKeycloakSession);
    }

    @Override
    public void init(Config.Scope config) {
        String kafkaHost = System.getenv(KAFKA_HOST);
        String kafkaPort = System.getenv(KAFKA_PORT);
        identityHost = System.getenv("IDENTITY_HOST");
        identityPort = System.getenv("IDENTITY_PORT");
        String ut = System.getenv("IDENTITY_UPDATE_TIMER");
        if (null != ut) {
            try {
                updateTime = Long.parseLong(ut);
            } catch (NumberFormatException nfe) {
                log.error(" Invalid value {} for variable IDENTITY_UPDATE_TIMER - using default", ut);
            }
        }
        updateTime *= 1000;

        if (null != kafkaHost && null != kafkaPort) {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(null);
            Properties properties = getProperties(kafkaHost, kafkaPort);
            try {
                producer = new KafkaProducer<>(properties);
                adapter = new KafkaAdapter(producer, identityHost, identityPort);
            } catch (Exception e) {
                log.error("cannot creat kafka producer {}", e.getMessage(), e);
            }
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        } else {
            log.info("Kafka Server not set");
        }

        String influxDBHost = getEnvOrDefault("MONITORING_INFLUXDB_HOST", "marvin.mid.de");
        String influxDBPort = getEnvOrDefault("MONITORING_INFLUXDB_PORT", "8086");
        String influxDBUser = getEnvOrDefault("INFLUXDB_USER", "smartfacts-monitoring-client");
        String influxDBSecret = System.getenv("MONITORING_INFLUXDB_SECRET");
        String influxdbDBName = System.getenv("MONITORING_ENVIRONMENT_NAME");
        String influxdbDBServiceName = getEnvOrDefault("IDENTITY_HOST", "identity");
        String influxdbRetentionPolicy = getEnvOrDefault("INFLUXDB_DB_RETENTION_POLICY", "");
        String influxUrl = String.format("https://%s:%s", influxDBHost, influxDBPort);


        if (null != influxdbDBName && !influxdbDBName.isEmpty()) {
            log.info("Connecting to InfluxDB URL: {} Databasename {} ServiceName {} RetentionPolicy {}", influxUrl, influxdbDBName, influxdbDBServiceName, influxdbRetentionPolicy);
            bpandaInfluxDBClient = BpandaInfluxDBClient.createBpandaInfluxDBClient(influxUrl, influxDBUser, influxDBSecret, influxdbDBName, influxdbDBServiceName, influxdbRetentionPolicy);
        } else {
            log.info("MONITORING_ENVIRONMENT_NAME not set - no influxdb connection");
        }
    }

    private static Properties getProperties(String kafkaHost, String kafkaPort) {
        Properties properties = new Properties();
        String bootstrapServer = String.format("https://%s:%s", kafkaHost, kafkaPort);
        properties.put("bootstrap.servers", bootstrapServer);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        properties.put("retries", 3); // retries on transient errors and load balancing disconnection
        properties.put("max.request.size", 1024 * 1024); // limit request size to 1MB            properties.put("bootstrap.servers", bootstrapServer);
        return properties;
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        KeycloakModelUtils.runJobInTransaction(keycloakSessionFactory, s1 -> {
            TimerProvider timer = s1.getProvider(TimerProvider.class);
            log.info("Registering send status update task with TimerProvider - updateTime = {}", updateTime);
            timer.schedule(() -> KeycloakModelUtils.runJobInTransaction(s1.getKeycloakSessionFactory(), s2 -> {
                log.debug("Sending status scheduler");
                this.sendStatusUpdateForSession(s2);
            }),  updateTime, "keycloakStatusTimer");
        });
    }

    @Override
    public void close() {
        if (null != bpandaInfluxDBClient) {
            bpandaInfluxDBClient.close();
        }
    }

    @Override
    public String getId() {
        return "Bpanda-event-listener";
    }


    private void sendStatusUpdateForSession(KeycloakSession session) {
        // nur jedes fünfte Mal senden, damit der erster Udate zeitnah kommt, ohne das System zu fluten
        if((++counter %5) == 0) {
            if (session != null && session.getContext() != null && this.adapter != null) {
                String allRealms = session.realms().getRealmsStream().map(RealmModel::getName).collect(Collectors.joining(","));
                long realmCount = session.realms().getRealmsStream().count();
                log.info("send StatusUpdate realmCount = {}", realmCount);

                this.adapter.sendStatusUpdate(realmCount, allRealms);
                if (bpandaInfluxDBClient != null && (counter % 20) == 0) {
                    log.info("log RealmCount to influx");
                    bpandaInfluxDBClient.logRealmCount(realmCount);
                }
            } else {
                log.debug("StatusUpdate not send - keycloakSession{}", session == null ? " is null" : " has no context");
            }
        }else {
            log.debug("StatusUpdate not send - keycloakSession count = {} timer {}", counter, updateTime);
        }
        if (counter >= Integer.MAX_VALUE/2) {
            counter = 0;
        }
    }
    private static String getEnvOrDefault(String name, String defaultValue) {
        String result = System.getenv(name);
        return result == null ? defaultValue : result;
    }
}
