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

    private long updateTime = 120;
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
                updateTime = Long.parseLong(ut) * 1000;
            } catch (NumberFormatException nfe) {
                log.error(" Invalid value " + ut + " for variable IDENTITY_UPDATE_TIMER - using default");
            }
        }

        if (null != kafkaHost && null != kafkaPort) {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(null);
            Properties properties = getProperties(kafkaHost, kafkaPort);
            try {
                producer = new KafkaProducer<>(properties);
                adapter = new KafkaAdapter(producer, identityHost, identityPort);
            } catch (Exception e) {
                log.error("cannot creat kafka producer " + e.getMessage(), e);
            }
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        } else {
            log.info("Kafka Server not set");
        }

        String influxDBHost = System.getenv("MONITORING_INFLUXDB_HOST");
        String influxDBPort = System.getenv("MONITORING_INFLUXDB_PORT");
        String influxDBSecret = System.getenv("MONITORING_INFLUXDB_SECRET");
        String influxDBUser = System.getenv("MONITORING_INFLUXDB_USER");

        if (null != influxDBHost && null != influxDBSecret) {
            if (null == influxDBPort) {
                influxDBPort = "8086";
            }
            if (null == influxDBUser) {
                influxDBUser = "smartfacts-monitoring-client";
            }
            String url = String.format("https://%s:%s", influxDBHost, influxDBPort);
            bpandaInfluxDBClient = new BpandaInfluxDBClient(url, influxDBUser, influxDBSecret);
        } else {
            log.info("Either MONITORING_INFLUXDB_HOST or MONITORING_INFLUXDB_SECRET not set");
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
//        KeycloakModelUtils.runJobInTransaction(keycloakSessionFactory, s0 -> {
//                    log.info("Sending initial status update");
//                    this.sendStatusUpdateForSession(s0);
//                });

        KeycloakModelUtils.runJobInTransaction(keycloakSessionFactory, s1 -> {
            TimerProvider timer = s1.getProvider(TimerProvider.class);
            log.info("Registering send status update task with TimerProvider");
            timer.schedule(() -> KeycloakModelUtils.runJobInTransaction(s1.getKeycloakSessionFactory(), s2 -> {
                log.info("Sending status update");
                this.sendStatusUpdateForSession(s2);
            }),  updateTime, "keycloakStatusTimer");
        });
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "Bpanda-event-listener";
    }


    private void sendStatusUpdateForSession(KeycloakSession session) {
        // nur jedes fünfte Mal senden, damit der erster Udate zeitnah kommt, ohne das System  zu fluten
        if (session != null && session.getContext() != null && (counter++ %5) == 0) {
            String allRealms = session.realms().getRealmsStream().map(RealmModel::getName).collect(Collectors.joining(","));
            long realmCount = session.realms().getRealmsStream().count();
            log.info(String.format("sendStatusUpdate realmCount = %d", realmCount));

            this.adapter.sendStatusUpdate(realmCount, allRealms);
        } else {
            log.info("sendStatusUpdate - keycloakSession is null or has no context");
        }
    }
}
