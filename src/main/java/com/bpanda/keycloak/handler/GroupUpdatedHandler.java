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

import java.time.Instant;

public class GroupUpdatedHandler implements IKeycloakEventHandler {
    private final ScimGroup scimGroup;
    private final KafkaAdapter kafkaAdapter;
    private final String realmName;

    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);


    public GroupUpdatedHandler(KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String represantation) {
        scimGroup = ScimGroup.getFromResource(represantation);
        realmName = keycloakData.getRealmName();
        this.kafkaAdapter = kafkaAdapter;
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String groupId = scimGroup.getId();
        for (GroupMember member : scimGroup.getMembers()) {
            System.err.println("Member " + member.getValue());
        }
        log.info(String.format("Group %s LDAP/id Id %s Operation Updated ", scimGroup, groupId));

        EventMessages.AffectedElement dataId = kafkaAdapter.createAffectedElement(
                EventMessages.ElementTypes.ELEMENT_GROUP_IDS, groupId);
        EventMessages.AffectedElement realmData = kafkaAdapter.createAffectedElement(EventMessages.ElementTypes.ELEMENT_REALM_NAME, this.realmName);

        EventMessages.Event ev = EventMessages.Event.newBuilder()
                .setEventType(EventMessages.EventTypes.EVENT_CAM_GROUP_PROPS_CHANGED)
                .setTimestamp(String.valueOf(Instant.now().toEpochMilli()))
                .addData(dataId)
                .addData(realmData)
                .build();

    }
    @Override
    public boolean isValid() {
        return scimGroup != null && scimGroup.isValid();
    }
}
