package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import com.bpanda.keycloak.model.ScimUser;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;

public class UserCreatedHandler implements IKeycloakEventHandler {

    private final ScimUser scimUser;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;


    public UserCreatedHandler(KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        scimUser = ScimUser.getFromResource(representation);
        realmName = keycloakData.getRealmName();
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String userId = null;
        if (scimUser != null && scimUser.isValid()) {
            userId = scimUser.getId();
        }
        if (userId != null) {
            EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                    EventMessages.ElementTypes.ELEMENT_USER_IDS, userId);
            kafkaAdapter.send(realmName, "users.added", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_ADDED, affectedElement );
        }
    }

    @Override
    public boolean isValid() {
        return scimUser != null  && scimUser.isValid();
    }
}
