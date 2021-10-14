package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.CamAdapter;
import com.bpanda.keycloak.eventlistener.CampException;
import com.bpanda.keycloak.model.CamUser;
import com.bpanda.keycloak.model.KeycloakData;
import com.bpanda.keycloak.model.ScimUser;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public class UserUpdatedHandler implements IKeycloakEventHandler {

    private final CamAdapter camAdapter;
    private final ScimUser scimUser;

    public UserUpdatedHandler(String campServer, String accountId, KeycloakData keycloakData, String representation) {
        camAdapter = new CamAdapter(campServer, accountId, keycloakData);
        scimUser = ScimUser.getFromResource(representation);
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) throws CampException, IOException {
        CamUser camUser = new CamUser(scimUser);
        camAdapter.updateUser(camUser);
    }

    @Override
    public boolean isValid() {
        return scimUser != null  && scimUser.isValid();
    }
}
