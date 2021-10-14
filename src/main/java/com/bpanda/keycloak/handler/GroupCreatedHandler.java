package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import com.bpanda.keycloak.eventlistener.CamAdapter;
import com.bpanda.keycloak.model.GroupMember;
import com.bpanda.keycloak.model.KeycloakData;
import com.bpanda.keycloak.model.ScimGroup;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupCreatedHandler implements IKeycloakEventHandler {
    private final CamAdapter camAdapter;
    private final ScimGroup scimGroup;
    private static final String LDAP_ID = "LDAP_ID";
    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);


    public GroupCreatedHandler(String campServer, String accountId, KeycloakData keycloakData, String represantation) {
        camAdapter = new CamAdapter(campServer, accountId, keycloakData);
        scimGroup = ScimGroup.getFromResource(represantation);
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        String externalId = scimGroup.getId();
        for (GroupMember member : scimGroup.getMembers()) {
            System.err.println("Member " + member.getValue());
        }
//        Object attributesObject = group.getAttributes();
//        if (attributesObject != null) {
//            HashMap<String, List<String>> attributes = (HashMap) attributesObject;
//            if (attributes.containsKey(LDAP_ID)) {
//                List<String> values = attributes.get(LDAP_ID);
//                if (values != null && !values.isEmpty()) {
//                    externalId = values.get(0);
//                }
//            }
//        }
        log.info(String.format("Group %s LDAP/id Id %s Operation Created ", scimGroup, externalId));
    }

    @Override
    public boolean isValid() {
        return scimGroup != null && scimGroup.isValid();
    }
}
