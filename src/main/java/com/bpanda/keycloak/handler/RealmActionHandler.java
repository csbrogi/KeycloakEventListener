package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.CampException;
import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.RealmAction;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RealmActionHandler implements IKeycloakEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);
    private final RealmAction realmAction;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;

    public RealmActionHandler(KafkaAdapter kafkaAdapter, String realmName, String representation) {
        realmAction = RealmAction.getFromResource(representation);
        this.realmName = realmName;
        this.kafkaAdapter = kafkaAdapter;

        if (realmAction != null) {
            String action = realmAction.getAction();

            log.info(String.format("REALM action %s changes %b => %s", action, realmAction.hasChanges(), realmAction));
        }
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) throws CampException, IOException {
        String action = realmAction.getAction();
        log.info(String.format("REALM action %s changes %b => %s", action, realmAction.hasChanges(), realmAction));
        kafkaAdapter.send(realmName, "users.synced", EventMessages.EventTypes.EVENT_KEYCLOAK_FULL_SYNC, null );
    }

    @Override
    public boolean isValid() {
        String action = realmAction.getAction();
        return action.equals("triggerChangedUsersSync") || action.equals("triggerFullSync");
    }
}
