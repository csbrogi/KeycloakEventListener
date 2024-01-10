package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.stream.Collectors;

public class RealmCreatedHandler implements IKeycloakEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);
    private final KafkaAdapter kafkaAdapter;

    private final KeycloakData keycloakData;

    public RealmCreatedHandler(KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String realmName, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        this.keycloakData = keycloakData;

        log.info(String.format("REALM created %s representation %s", realmName, representation));
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) throws IOException {
        log.info(String.format("REALM created keycloak %s", keycloakData.getKeycloakServer()));
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
