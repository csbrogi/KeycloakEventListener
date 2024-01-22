package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.GroupMember;
import com.bpanda.keycloak.model.ScimGroup;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupUpdatedHandler implements IKeycloakEventHandler {
    private final ScimGroup scimGroup;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;

    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);

    public GroupUpdatedHandler(KafkaAdapter kafkaAdapter, String realmName, String representation) {
        scimGroup = ScimGroup.getFromResource(representation);
        this.realmName = realmName;
        this.kafkaAdapter = kafkaAdapter;
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String groupId = scimGroup.getId();
//        for (GroupMember member : scimGroup.getMembers()) {
//            log.trace("Member " + member.getValue());
//        }
        log.info(String.format("Group %s LDAP/id Id %s Operation Updated Members %d ", scimGroup.getDisplayName(), groupId, scimGroup.getMembers().stream().count()));

        EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(
                EventMessages.ElementTypes.ELEMENT_GROUP_IDS, groupId);

        kafkaAdapter.send(realmName, "groups.changed", EventMessages.EventTypes.EVENT_KEYCLOAK_GROUPS_CHANGED, affectedElement );
    }

    @Override
    public boolean isValid() {
        return scimGroup != null && scimGroup.isValid();
    }
}
