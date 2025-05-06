package com.bpanda.keycloak.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScimGroup {
    private String id;
    private String externalId;
    private String displayName;
    private List<GroupMember> members = new ArrayList<>();
    private ScimMetaData meta;

     public static ScimGroup getFromResource(String representation) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return objectMapper.readValue(representation, ScimGroup.class);
        } catch (IOException ignored) {
        }
        return null;
    }

    public ScimGroup() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<GroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMember> members) {
        this.members = members;
    }

    public ScimMetaData getMeta() {
        return meta;
    }

    public void setMeta(ScimMetaData meta) {
        this.meta = meta;
    }

    @Override
    public String toString() {
        return "ScimGroup{" +
                "id='" + id + '\'' +
                ", externalId='" + externalId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", members=" + members +
                ", meta=" + meta +
                '}';
    }

    public boolean isValid() {
         return id != null && displayName != null;
    }
}
