package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.ScimUser;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;

import java.net.URI;

public class UserDeletedHandler implements IKeycloakEventHandler {

    private final KafkaAdapter kafkaAdapter;
    private final String realmName;
    private final String userId;


    public UserDeletedHandler(KafkaAdapter kafkaAdapter, String realmName, URI uri, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        ScimUser scimUser = ScimUser.getFromResource(representation);
        this.realmName = realmName;
        if (scimUser != null) {
            if (scimUser.getExternalId() != null) {
                userId = scimUser.getExternalId();
            } else {
                userId = scimUser.getLdapId();
            }
        } else {
            // this wouldn't work since the userId is a keycloak-Id not the cam-Id
//            int pos = uri.getRawPath().lastIndexOf("/");
//            if (pos > 0) {
//                userId = uri.getRawPath().substring(pos + 1);
//            } else {
//                userId = null;
//            }
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
        return userId != null && !userId.isEmpty();
    }
}
