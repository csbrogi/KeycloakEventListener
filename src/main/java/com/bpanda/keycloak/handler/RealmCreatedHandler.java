package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class RealmCreatedHandler implements IKeycloakEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;

    private final KeycloakData keycloakData;

    public RealmCreatedHandler(KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String realmName, String representation) {
        this.realmName = realmName;
        this.kafkaAdapter = kafkaAdapter;
        this.keycloakData = keycloakData;

        log.info(String.format("REALM created %s representation %s", this.realmName, representation));
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) throws IOException {
        log.info(String.format("REALM created keycloak %s %d", keycloakData.getKeycloakServer(), keycloakSession.realms().getRealmsStream().count()));

        if (null != keycloakData.getKeycloakServer()) {
            KeycloakUriInfo uri =  keycloakSession.getContext().getUri();
            kafkaAdapter.sendStatusUpdate(keycloakSession, uri);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
