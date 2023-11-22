package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RealmCreatedHandler implements IKeycloakEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);
//    private final RealmAction realmAction;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;

    private final KeycloakData keycloakData;

    public RealmCreatedHandler(KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String realmName, String representation) {
        this.realmName = realmName;
        this.kafkaAdapter = kafkaAdapter;
        this.keycloakData = keycloakData;

//        if (realmAction != null) {
//            String action = realmAction.getAction();

            log.info(String.format("REALM created %s representation %s", this.realmName, representation));
//        }
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) throws IOException {
//        String action = realmAction.getAction();
        log.info(String.format("REALM created keycloak %s %d", keycloakData.getKeycloakServer(), keycloakSession.realms().getRealmsStream().count()));

//        kafkaAdapter.send(realmName, "users.synced", EventMessages.EventTypes.EVENT_KEYCLOAK_FULL_SYNC, null);
    }

    @Override
    public boolean isValid() {
        return true;
//        String action = realmAction.getAction();
//        return action.equals("triggerChangedUsersSync") || action.equals("triggerFullSync");
    }
}
