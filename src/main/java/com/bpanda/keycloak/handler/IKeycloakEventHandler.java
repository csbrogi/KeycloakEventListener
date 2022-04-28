package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.CampException;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public interface IKeycloakEventHandler {
    /**
     * handle the event
     * @param keycloakSession keycloak session
     * @throws CampException
     * @throws IOException
     */
    void handleRequest(KeycloakSession keycloakSession) throws CampException, IOException;

    /**
     * checks if the event can be evaluated
     * @return
     */
    boolean isValid();
}
