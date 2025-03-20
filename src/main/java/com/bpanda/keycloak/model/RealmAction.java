package com.bpanda.keycloak.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class RealmAction {
    public RealmAction() {
    }

    public static RealmAction getFromResource(String representation) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return objectMapper.readValue(representation, RealmAction.class);
        } catch (IOException ignored) {
        }
        return null;
    }

    private String action;

    private UserSyncResult result;

    public boolean hasChanges() {
        return result.hasChanges();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public UserSyncResult getResult() {
        return result;
    }

    public void setResult(UserSyncResult result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "RealmAction{" +
                "action='" + action + '\'' +
                ", result=" + result.toString() +
                '}';
    }
}
