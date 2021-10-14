package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.CamAdapter;
import com.bpanda.keycloak.eventlistener.CampException;
import com.bpanda.keycloak.model.KeycloakData;
import com.bpanda.keycloak.model.RealmAction;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RealmActionHandler implements IKeycloakEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);
    private final RealmAction realmAction;
    private final CamAdapter camAdapter;

    public RealmActionHandler(String campServer, String accountId, KeycloakData keycloakData, String representation) {
        camAdapter = new CamAdapter(campServer, accountId, keycloakData);
        realmAction = RealmAction.getFromResource(representation);

        if (realmAction != null) {
            String action = realmAction.getAction();
            log.info(String.format("REALM action %s changes %b => %s", action, realmAction.hasChanges(), realmAction));
        }
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) throws CampException, IOException {
        String action = realmAction.getAction();
        log.info(String.format("REALM action %s changes %b => %s", action, realmAction.hasChanges(), realmAction));
        camAdapter.startUserSync();
    }

    @Override
    public boolean isValid() {
        String action = realmAction.getAction();
        return action.equals("triggerChangedUsersSync") || action.equals("triggerFullSync");
    }
}
