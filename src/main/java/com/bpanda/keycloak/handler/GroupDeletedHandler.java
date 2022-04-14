package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.GroupMember;
import com.bpanda.keycloak.model.ScimGroup;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupDeletedHandler implements IKeycloakEventHandler {
    private final KafkaAdapter kafkaAdapter;
    private final ScimGroup scimGroup;
    private final String realmName;
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);


    public GroupDeletedHandler(KafkaAdapter kafkaAdapter, String realmName, String representation) {
        this.kafkaAdapter = kafkaAdapter;
        this.realmName = realmName;
        scimGroup = ScimGroup.getFromResource(representation);
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String externalId = scimGroup.getId();
        for (GroupMember member : scimGroup.getMembers()) {
            log.trace("Member " + member.getValue());
        }
        EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(EventMessages.ElementTypes.ELEMENT_GROUP_IDS, externalId);

        kafkaAdapter.send(realmName, "groups.deleted", EventMessages.EventTypes.EVENT_KEYCLOAK_GROUPS_DELETED, affectedElement );
        log.info(String.format("Group %s LDAP/id Id %s Operation Deleted ", scimGroup, externalId));
    }

    @Override
    public boolean isValid() {
        return scimGroup != null && scimGroup.isValid();
    }
}
