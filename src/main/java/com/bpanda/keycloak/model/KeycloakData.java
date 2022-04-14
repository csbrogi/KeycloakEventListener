package com.bpanda.keycloak.model;

public class KeycloakData {
    private String keycloakServer;
    private String clientId;
    private String clientSecret;
    private String realmName;

    public KeycloakData(String keycloakServer, String clientId, String clientSecret, String realmName) {
        this.keycloakServer = keycloakServer;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.realmName = realmName;
    }

    public static KeycloakData create(String keycloakServer, String realmId, String clientId, String clientSecret) {
        return  new KeycloakData(keycloakServer, clientId, clientSecret, realmId);
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getKeycloakServer() {
        return keycloakServer;
    }

    public void setKeycloakServer(String keycloakServer) {
        this.keycloakServer = keycloakServer;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
