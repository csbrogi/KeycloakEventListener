package com.bpanda.keycloak.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class Operation {
    public static List<Operation> getFromResource(String representation) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        try {
            return objectMapper.readValue(representation, new TypeReference<List<Operation>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Operation() {
    }

    public Operation(String op, String path) {
        this.op = op;
        this.path = path;
    }

    public Operation(String fromValue) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            JsonNode actualObj = objectMapper.readTree(fromValue);
            JsonNode jsonNode1 = actualObj.get("op");
            if (jsonNode1 != null) {
                this.op = jsonNode1.textValue();
            }
            jsonNode1 = actualObj.get("path");
            if (jsonNode1 != null) {
                this.path = jsonNode1.textValue();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

//    public List<Boolean> getValue() {
//        return value;
//    }
//
//    public void setValue(List<Boolean> value) {
//        this.value = value;
//    }

    private String op;
    private String path;
//    private List<Boolean> value;
}
