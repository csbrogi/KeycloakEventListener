package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.CamUser;
import com.bpanda.keycloak.model.KeycloakData;
import com.bpanda.keycloak.model.ScimUser;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;

public class UserUpdatedHandler implements IKeycloakEventHandler {

    private final ScimUser scimUser;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;


    public UserUpdatedHandler(KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        realmName = keycloakData.getRealmName();
        scimUser = ScimUser.getFromResource(representation);
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                EventMessages.ElementTypes.ELEMENT_USER_IDS, scimUser.getId());
        kafkaAdapter.send(realmName, "users.updated", EventMessages.EventTypes.EVENT_CAM_USERS_CHANGED, affectedElement );
    }

    @Override
    public boolean isValid() {
        return scimUser != null  && scimUser.isValid();
    }
}
