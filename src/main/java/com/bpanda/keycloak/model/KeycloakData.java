package com.bpanda.keycloak.model;

public class KeycloakData {
    private String keycloakServer;
    private String realmName;

    public KeycloakData(String keycloakServer, String realmName) {
        this.keycloakServer = keycloakServer;
        this.realmName = realmName;
    }

    public static KeycloakData create(String keycloakServer, String realmId) {
        return  new KeycloakData(keycloakServer, realmId);
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

}
