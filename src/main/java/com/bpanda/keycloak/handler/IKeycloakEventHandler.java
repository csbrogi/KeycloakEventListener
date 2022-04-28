package com.bpanda.keycloak.handler;

import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public interface IKeycloakEventHandler {
    /**
     * handle the event
     * @param keycloakSession keycloak session
     */
    void handleRequest(KeycloakSession keycloakSession) throws IOException;

    /**
     * checks if the event can be evaluated
     * @return test result
     */
    boolean isValid();
}
