package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.GroupMember;
import com.bpanda.keycloak.model.KeycloakData;
import com.bpanda.keycloak.model.ScimGroup;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupCreatedHandler implements IKeycloakEventHandler {
    private final KafkaAdapter kafkaAdapter;
    private final ScimGroup scimGroup;
    private final String realmName;
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);


    public GroupCreatedHandler(KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String represantation) {
        this.kafkaAdapter = kafkaAdapter;
        realmName = keycloakData.getRealmName();
        scimGroup = ScimGroup.getFromResource(represantation);
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String externalId = scimGroup.getId();
        for (GroupMember member : scimGroup.getMembers()) {
            System.err.println("Member " + member.getValue());
        }
        EventMessages.AffectedElement affectedElement = kafkaAdapter.createAffectedElement(EventMessages.ElementTypes.ELEMENT_GROUP_ID, externalId);

        kafkaAdapter.send(realmName, "groups.added", EventMessages.EventTypes.EVENT_CAM_GROUPS_ADDED, affectedElement );
        log.info(String.format("Group %s LDAP/id Id %s Operation Created ", scimGroup, externalId));
    }

    @Override
    public boolean isValid() {
        return scimGroup != null && scimGroup.isValid();
    }
}
