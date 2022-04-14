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

import java.util.Properties;

public class BpandaEventListenerProviderFactory implements EventListenerProviderFactory{
    private String campServer  = null;
    private KafkaProducer producer;

    private static final String KAFKA_HOST = "KAFKA_HOST";
    private static final String KAFKA_PORT = "KAFKA_PORT";
    private static final String SUBSCRIPTION_HOST = "SUBSCRIPTION_HOST";
    private static final String SUBSCRIPTION_PORT = "SUBSCRIPTION_PORT";
    private static final String SUBSCRIPTION = "SUBSCRIPTION";

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new BpandaEventListenerProvider(producer, campServer, keycloakSession);
    }

    @Override
    public void init(Config.Scope config) {
        System.err.println("Scope: " + config.toString());
        String subscriptionHost = System.getenv(SUBSCRIPTION_HOST);
        String subscriptionPort = System.getenv(SUBSCRIPTION_PORT);
        String subscription = System.getenv(SUBSCRIPTION);
        if (subscription != null && subscription.length() > 0) {
            campServer = subscription;
        } else if (null != subscriptionPort && subscriptionHost.length() > 0) {
            if ("443".equals(subscriptionPort)) {
                campServer = "https://" + subscriptionHost;
            } else {
                campServer = String.format("https://%s:%s", subscriptionHost, subscriptionPort);
            }
        } else {
            System.err.println("SUBSCRIPTION_HOST not set");
        }
        String kafkaHost = System.getenv(KAFKA_HOST);
        String kafkaPort = System.getenv(KAFKA_PORT);
        if (null != kafkaHost && null != kafkaPort) {
            ClassLoader oldClassLoader =  Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(null);
            Properties properties = new Properties();
            String bootstrapServer = String.format("https://%s:%s", kafkaHost, kafkaPort);
            properties.put("bootstrap.servers", bootstrapServer);
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

            properties.put("retries", 3); // retries on transient errors and load balancing disconnection
            properties.put("max.request.size", 1024 * 1024); // limit request size to 1MB            properties.put("bootstrap.servers", bootstrapServer);
            try {
                producer = new KafkaProducer<>(properties);
            } catch (Exception e) {
                System.err.println("cannot creat kafka producer " + e.getMessage());
                e.printStackTrace();
            }
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        } else {
            System.err.println("Kafka Server not set");
        }
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
