package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakUser;
import com.bpanda.keycloak.model.ScimUser;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class UserDeletedHandler implements IKeycloakEventHandler {

    private final ScimUser scimUser;
    private final KeycloakUser keycloakUser;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;


    public UserDeletedHandler(KafkaAdapter kafkaAdapter, String realmName, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        scimUser = ScimUser.getFromResource(representation);
        this.realmName = realmName;
        if (scimUser == null || !scimUser.isValid()) {
            keycloakUser = KeycloakUser.getFromResource(representation);
        } else {
            keycloakUser = null;
        }
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String userId = null;
        if (scimUser != null) {
            userId = scimUser.getId();
        } else if (null != keycloakUser && keycloakUser.isValid()) {
            RealmModel realm = keycloakSession.realms().getRealm(realmName);
            UserModel user = keycloakSession.users().getUserByEmail(realm, keycloakUser.getEmail());
            if (user != null) {
                userId = user.getId();
            }
        }
        if (userId != null) {
            EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                    EventMessages.ElementTypes.ELEMENT_USER_IDS, userId);
            kafkaAdapter.send(realmName, "users.deleted", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_DELETED, affectedElement );
        }
    }

    @Override
    public boolean isValid() {
        return scimUser != null  && scimUser.getId() != null && !scimUser.getId().equals("") ||
                keycloakUser != null && keycloakUser.isValid();
    }
}
