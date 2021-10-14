package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.CamAdapter;
import com.bpanda.keycloak.eventlistener.CampException;
import com.bpanda.keycloak.model.KeycloakData;
import com.bpanda.keycloak.model.KeycloakUser;
import com.bpanda.keycloak.model.ScimUser;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class UserCreatedHandler implements IKeycloakEventHandler {

    private final CamAdapter camAdapter;
    private final ScimUser scimUser;
    private final KeycloakUser keycloakUser;
    private final KeycloakData keycloakData;

    public UserCreatedHandler(String campServer, String accountId, KeycloakData keycloakData, String representation) {
        this.keycloakData = keycloakData;
        camAdapter = new CamAdapter(campServer, accountId, keycloakData);
        scimUser = ScimUser.getFromResource(representation);
        if (scimUser == null || !scimUser.isValid()) {
            keycloakUser = KeycloakUser.getFromResource(representation);
        } else {
            keycloakUser = null;
        }
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) throws CampException, IOException {
        if (scimUser != null && scimUser.isValid()) {
            camAdapter.createUser(scimUser.getEmail());
        } else if (null != keycloakUser && keycloakUser.isValid()) {
            RealmModel realm = keycloakSession.realms().getRealm(keycloakData.getRealmName());
            UserModel user = keycloakSession.users().getUserByEmail(keycloakUser.getEmail(), realm);
            if (user != null) {
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
    }

    @Override
    public boolean isValid() {
        return scimUser != null  && scimUser.isValid() ||
                keycloakUser != null && keycloakUser.isValid();
    }
}
