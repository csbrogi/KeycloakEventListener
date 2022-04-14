package com.bpanda.keycloak.model;

public class ScimMetaData {
    private  String resourceType;
    private String location;

    public ScimMetaData() {
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "ScimMetaData{" +
                "resourceType='" + resourceType + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
