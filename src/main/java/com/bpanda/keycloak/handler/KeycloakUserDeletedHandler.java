package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;

import java.net.URI;

public class KeycloakUserDeletedHandler implements IKeycloakEventHandler {

    private final KafkaAdapter kafkaAdapter;
    private final String realmName;
    private final String userId;


    public KeycloakUserDeletedHandler(KafkaAdapter kafkaAdapter, String realmName, URI uri, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        this.realmName = realmName;
        int pos = uri.getRawPath().lastIndexOf("/");
        if (pos > 0) {
            userId = uri.getRawPath().substring(pos + 1);
        } else {
            userId = null;
        }
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        if (userId != null) {
            EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                    EventMessages.ElementTypes.ELEMENT_USER_IDS, userId);
            kafkaAdapter.send(realmName, "users.deleted", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_DELETED, affectedElement );
        }
    }

    @Override
    public boolean isValid() {
        return userId != null && !userId.equals("");
    }
}
