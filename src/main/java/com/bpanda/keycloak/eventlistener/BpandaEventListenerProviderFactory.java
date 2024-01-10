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
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Spi;
import org.keycloak.timer.TimerProvider;
import org.keycloak.urls.HostnameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class BpandaEventListenerProviderFactory implements EventListenerProviderFactory {
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProviderFactory.class);
    private KafkaProducer producer;

    private KafkaAdapter adapter;

    private BpandaInfluxDBClient bpandaInfluxDBClient;

    private static final String KAFKA_HOST = "KAFKA_HOST";
    private static final String KAFKA_PORT = "KAFKA_PORT";

    private KeycloakSession keycloakSession;

    @Override
    public EventListenerProvider create(KeycloakSession aKeycloakSession) {
        if (aKeycloakSession != null && aKeycloakSession.getContext() != null) {
            try {
                aKeycloakSession.getContext().getUri();
                this.keycloakSession = aKeycloakSession;

            } catch (Exception e){
                log.error("create: aKeycloakSession.getContext().getUri(); failed ", e);
            }
        }
        return new BpandaEventListenerProvider(producer, bpandaInfluxDBClient, keycloakSession);
    }

    @Override
    public void init(Config.Scope config) {
        String kafkaHost = System.getenv(KAFKA_HOST);
        String kafkaPort = System.getenv(KAFKA_PORT);
        if (null != kafkaHost && null != kafkaPort) {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(null);
            Properties properties = getProperties(kafkaHost, kafkaPort);
            try {
                producer = new KafkaProducer<>(properties);
                adapter = new KafkaAdapter(producer);
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

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "Bpanda-event-listener";
    }


}
