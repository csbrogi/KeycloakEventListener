package com.bpanda.keycloak.model;

public class KeycloakData {
    private String keycloakServer;
    private String clientSecret;
    private String realmName;

    public KeycloakData(String keycloakServer, String clientSecret, String realmName) {
        this.keycloakServer = keycloakServer;
        this.clientSecret = clientSecret;
        this.realmName = realmName;
    }

    public static KeycloakData create(String keycloakServer, String realmId, String clientSecret) {
        return  new KeycloakData(keycloakServer, clientSecret, realmId);
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

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
