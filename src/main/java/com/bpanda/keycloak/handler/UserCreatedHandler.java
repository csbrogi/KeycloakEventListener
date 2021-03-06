package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import com.bpanda.keycloak.model.KeycloakUser;
import com.bpanda.keycloak.model.ScimUser;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class UserCreatedHandler implements IKeycloakEventHandler {

    private final ScimUser scimUser;
    private final KeycloakUser keycloakUser;
    private final KeycloakData keycloakData;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;


    public UserCreatedHandler(KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String representation) {
        this.keycloakData = keycloakData;
        this.kafkaAdapter = kafkaAdapter;
        scimUser = ScimUser.getFromResource(representation);
        realmName = keycloakData.getRealmName();
        if (scimUser == null || !scimUser.isValid()) {
            keycloakUser = KeycloakUser.getFromResource(representation);
        } else {
            keycloakUser = null;
        }
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String userId = null;
        if (scimUser != null && scimUser.isValid()) {
            userId = scimUser.getId();
        } else if (null != keycloakUser && keycloakUser.isValid()) {
            RealmModel realm = keycloakSession.realms().getRealm(keycloakData.getRealmName());
            UserModel user = keycloakSession.users().getUserByEmail(realm, keycloakUser.getEmail());
            if (user != null) {
                userId = user.getId();
                try {
                    ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmms.'0'X");
                    user.setSingleAttribute("createTimestamp", zdt.format(formatter));
                } catch (DateTimeException ex) {
                    ex.printStackTrace();
                    user.setSingleAttribute("createTimestamp", DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.now()));
                }
            }
        }
        if (userId != null) {
            EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                    EventMessages.ElementTypes.ELEMENT_USER_IDS, userId);
            kafkaAdapter.send(realmName, "users.added", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_ADDED, affectedElement );
        }
    }

    @Override
    public boolean isValid() {
        return scimUser != null  && scimUser.isValid() ||
                keycloakUser != null && keycloakUser.isValid();
    }
}
