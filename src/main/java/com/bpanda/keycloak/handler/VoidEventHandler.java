package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.BpandaEventListenerProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoidEventHandler implements IKeycloakEventHandler {
    private final ResourceType resourceType;
    private final OperationType operationType;
    private final String realmId;

    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);

    public VoidEventHandler(ResourceType resourceType, OperationType operationType, String realmId) {
        this.realmId = realmId;
        this.resourceType = resourceType;
        this.operationType = operationType;
    }

    @Override
    public void handleRequest(KeycloakSession keycloakSession) {
        log.info(this.toString());
    }

    @Override
    public String toString() {
        return new StringBuilder().append("VoidEventHandler{").append("resourceType=").append(resourceType).append(", operationType=").append(operationType).append(", realmId='").append(realmId).append('\'').append('}').toString();
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
