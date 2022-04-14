package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.CampException;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public interface IKeycloakEventHandler {
    void handleRequest(KeycloakSession keycloakSession) throws CampException, IOException;
    boolean isValid();
}
