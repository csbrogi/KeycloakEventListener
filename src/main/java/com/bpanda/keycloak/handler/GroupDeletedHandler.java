package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.ScimGroup;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class GroupDeletedHandler implements IKeycloakEventHandler {
    private final KafkaAdapter kafkaAdapter;
    private final ScimGroup scimGroup;
    private final String groupId;
    private final String realmName;
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);


    public GroupDeletedHandler(KafkaAdapter kafkaAdapter, String realmName, URI uri, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        this.realmName = realmName;
        int pos = uri.getRawPath().lastIndexOf("/");
        if (pos > 0) {
            groupId = uri.getRawPath().substring(pos + 1);
        } else {
            groupId = null;
        }
        scimGroup = ScimGroup.getFromResource(representation);
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(EventMessages.ElementTypes.ELEMENT_GROUP_NAME, scimGroup.getDisplayName());

        kafkaAdapter.send(realmName, "groups.deleted", EventMessages.EventTypes.EVENT_KEYCLOAK_GROUPS_DELETED, affectedElement );
        log.info("Group LDAP/id Id {} Operation Deleted ", groupId);
    }

    @Override
    public boolean isValid() {
        return groupId != null && !groupId.isEmpty();
    }
}
