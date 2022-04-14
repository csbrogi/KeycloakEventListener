package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.ScimUser;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;

public class UserUpdatedHandler implements IKeycloakEventHandler {

    private final ScimUser scimUser;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;


    public UserUpdatedHandler(KafkaAdapter kafkaAdapter, String realmName, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        this.realmName = realmName;
        scimUser = ScimUser.getFromResource(representation);
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                EventMessages.ElementTypes.ELEMENT_USER_IDS, scimUser.getId());
        kafkaAdapter.send(realmName, "users.updated", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_CHANGED, affectedElement );
    }

    @Override
    public boolean isValid() {
        return scimUser != null  && scimUser.isValid();
    }
}
