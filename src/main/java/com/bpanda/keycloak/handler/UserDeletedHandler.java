package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakUser;
import com.bpanda.keycloak.model.ScimUser;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;

import java.net.URI;

public class UserDeletedHandler implements IKeycloakEventHandler {

    private final KafkaAdapter kafkaAdapter;
    private final ScimUser scimUser;
    private final KeycloakUser keycloakUser;
    private final String realmName;
    private final String userId;


    public UserDeletedHandler(KafkaAdapter kafkaAdapter, String realmName, URI uri, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        this.realmName = realmName;
        int pos = uri.getRawPath().lastIndexOf("/");
        if (pos > 0) {
            userId = uri.getRawPath().substring(pos + 1);
        } else {
            userId = null;
        }
        scimUser = ScimUser.getFromResource(representation);
        if (scimUser == null || !scimUser.isValid()) {
            keycloakUser = KeycloakUser.getFromResource(representation);
        } else {
            keycloakUser = null;
        }
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String email = null;
        if (keycloakUser != null) {
            email = keycloakUser.getEmail();
        } else if (scimUser != null) {
            email = scimUser.getEmail();
        }
        if (email != null) {
            EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                    EventMessages.ElementTypes.ELEMENT_USER_IDS_MAILS, email);
            kafkaAdapter.send(realmName, "users.deleted", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_DELETED, affectedElement );
        }
    }

    @Override
    public boolean isValid() {
        return userId != null && !userId.equals("");
    }
}
