package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.stream.Collectors;

public class RealmHandler implements IKeycloakEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);
    private final KafkaAdapter kafkaAdapter;
    private final String operationType;
    private final KeycloakData keycloakData;

    public RealmHandler(KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String realmName, OperationType operationType, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        this.keycloakData = keycloakData;
        this.operationType = operationType.toString();

        log.info("REALM {} {} representation {}", this.operationType, realmName, representation);
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) throws IOException {
        log.info("REALM {} keycloak {}", operationType, keycloakData.getKeycloakServer());
        try {
            String allRealms = keycloakSession.realms().getRealmsStream().map(RealmModel::getName).collect(Collectors.joining(","));
            long realmCount = keycloakSession.realms().getRealmsStream().count();
            kafkaAdapter.sendStatusUpdate(realmCount, allRealms);
        } catch (Exception e) {
            log.error("could not send status update: Exception - caught", e);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
