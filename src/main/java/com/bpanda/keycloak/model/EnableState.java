package com.bpanda.keycloak.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class EnableState {
     public static EnableState getFromResource(String representation) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            EnableState enableState = objectMapper.readValue(representation, EnableState.class);
            return enableState;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean isEnabled() {
        return enabled;
    }

    public EnableState() {
    }

    public EnableState(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private boolean enabled;
}
