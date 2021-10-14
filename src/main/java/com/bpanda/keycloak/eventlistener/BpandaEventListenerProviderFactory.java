package com.bpanda.keycloak.eventlistener;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class BpandaEventListenerProviderFactory implements EventListenerProviderFactory{
    private String campServer  = null;

    private static final String SUBSCRIPTION_HOST = "SUBSCRIPTION_HOST";
    private static final String SUBSCRIPTION_PORT = "SUBSCRIPTION_PORT";

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new BpandaEventListenerProvider(campServer, keycloakSession);
    }

    @Override
    public void init(Config.Scope config) {
        System.err.println("Scope: " + config.toString());
        String subscriptionHost = System.getenv(SUBSCRIPTION_HOST);
        String subscriptionPort = System.getenv(SUBSCRIPTION_PORT);
        if (null != subscriptionPort && subscriptionHost.length() > 0) {
            if ("443".equals(subscriptionPort)) {
                campServer = "https://" + subscriptionHost;
            } else {
                campServer = String.format("https://%s:%s", subscriptionHost, subscriptionPort);
            }
        } else {
            System.err.println("SUBSCRIPTION_HOST not set");
        }

    }
    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "Bpanda-event-listener";
    }
}
