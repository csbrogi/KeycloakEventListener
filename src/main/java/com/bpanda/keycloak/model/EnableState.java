package com.bpanda.keycloak.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class EnableState {
    /**
     * @return the enable state, or {@code null} if the representation is not a JSON object
     * containing a boolean "enabled" property
     */
     public static EnableState getFromResource(String representation) {
        if (representation == null || representation.isEmpty()) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            JsonNode node = objectMapper.readTree(representation);
            if (node == null || !node.isObject() || !node.path("enabled").isBoolean()) {
                return null;
            }
            return new EnableState(node.get("enabled").booleanValue());
        } catch (IOException ignored) {
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
