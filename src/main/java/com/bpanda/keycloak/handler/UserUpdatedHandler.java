package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.EnableState;
import com.bpanda.keycloak.model.KeycloakUser;
import com.bpanda.keycloak.model.Operation;
import com.bpanda.keycloak.model.ScimUser;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserUpdatedHandler implements IKeycloakEventHandler {

    private final KeycloakUser keycloakUser;
    private final ScimUser scimUser;
    private final EnableState enableState;
    private final List<Operation> operations;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;
    private final String userId;


    public UserUpdatedHandler(KafkaAdapter kafkaAdapter, String realmName, URI uri, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        this.realmName = realmName;
        keycloakUser = KeycloakUser.getFromResource(representation);
        scimUser = ScimUser.getFromResource(representation);
        enableState = EnableState.getFromResource(representation);
        operations = Operation.getFromResource(representation);
        int pos = uri.getRawPath().lastIndexOf("/");
        if (pos > 0) {
            userId = uri.getRawPath().substring(pos + 1);
        } else {
            userId = null;
        }
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String updateUserId = null;
        if (scimUser != null) {
            updateUserId = scimUser.getId();
        }
        if (userId != null) {
            RealmModel realmModel = keycloakSession.realms().getRealm(realmName);
            UserModel user = keycloakSession.users().getUserById(realmModel, userId);
            String val = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            if (user != null) {
                String attr = "lastModifiedTimestamp";
                if (enableState != null) {
                    if (enableState.isEnabled()) {
                        attr = "lastEnableTimestamp";
                    } else {
                        attr = "lastDisableTimestamp";
                    }
                } else if (operations != null && !operations.isEmpty()) {
                    updateUserId = userId;

//                    Operation operation = operations.get(0);
//                    if (operation.getPath().equalsIgnoreCase("active") && operation.getValue() != null && !operation.getValue().isEmpty()) {
//                        if (operation.getValue().get(0)) {
//                            attr = "lastEnableTimestamp";
//                        } else {
//                            attr = "lastDisableTimestamp";
//                        }
//                    }
                }
                user.setSingleAttribute(attr, val);
            }
        }

        if (updateUserId != null) {
            EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                    EventMessages.ElementTypes.ELEMENT_USER_IDS, updateUserId);
            kafkaAdapter.send(realmName, "users.updated", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_CHANGED, affectedElement);
        }
    }

    @Override
    public boolean isValid() {
        if (enableState != null) return true;
        if (operations != null && !operations.isEmpty()) return operations.get(0).getPath() != null;
        return scimUser != null  && scimUser.isValid() ||
                keycloakUser != null && keycloakUser.isValid();    }
}
