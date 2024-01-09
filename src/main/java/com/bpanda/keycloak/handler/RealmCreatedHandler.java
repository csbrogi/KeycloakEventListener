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
        try {
            String allRealms = keycloakSession.realms().getRealmsStream().map(RealmModel::getName).collect(Collectors.joining(","));
            long realmCount = keycloakSession.realms().getRealmsStream().count();
            kafkaAdapter.sendStatusUpdate(realmCount, allRealms);
        } catch (Exception e) {
            log.error("could not send status update: Exception - caught", e);
        }
//        log.info(String.format("REALM created keycloak %s %d", keycloakData.getKeycloakServer(), keycloakSession.realms().getRealmsStream().count()));

//        if (null != keycloakData.getKeycloakServer()) {
//            KeycloakUriInfo uri =  keycloakSession.getContext().getUri();
//            String allRealms = keycloakSession.realms().getRealmsStream().map(RealmModel::getName).collect(Collectors.joining(","));
//
//            kafkaAdapter.sendStatusUpdate(keycloakSession, uri, allRealms);
//        }
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
