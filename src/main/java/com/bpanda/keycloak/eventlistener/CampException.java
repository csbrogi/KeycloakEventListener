package com.bpanda.keycloak.eventlistener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CampException extends Exception{
    public int responseCode;

    private String scimError;

    public CampException(String message, int responseCode) {
        super(message);
        try {
            this.responseCode = responseCode;
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            JsonNode jsonError = mapper.readTree(message);
            JsonNode detail = jsonError.get("detail");
            if (detail != null) {
                this.scimError = detail.asText();
            }
        } catch (IllegalArgumentException ex) {
            this.responseCode = 500;
        } catch (JsonProcessingException e) {
            this.scimError = message;
        }
        System.err.println("Created Exception " + this.getMessage());
    }

    public CampException(Exception ex) {
        super(ex.getMessage(), ex);
        this.responseCode = 500;
    }


    public CampException(String message) {
        super(message);
        this.responseCode = 500;
    }
}
