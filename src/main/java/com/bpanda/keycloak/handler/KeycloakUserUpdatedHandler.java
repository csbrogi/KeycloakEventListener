package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakUser;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;

public class KeycloakUserUpdatedHandler implements IKeycloakEventHandler {

    private final KeycloakUser keycloakUser;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;


    public KeycloakUserUpdatedHandler(KafkaAdapter kafkaAdapter, String realmName, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        this.realmName = realmName;
        keycloakUser = KeycloakUser.getFromResource(representation);
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                EventMessages.ElementTypes.ELEMENT_USER_IDS, keycloakUser.getId());
        kafkaAdapter.send(realmName, "users.updated", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_CHANGED, affectedElement );
    }

    @Override
    public boolean isValid() {
        return keycloakUser != null  && keycloakUser.isValid();
    }
}
