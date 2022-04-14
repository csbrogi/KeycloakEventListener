package com.bpanda.keycloak.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class GroupMember {
    private String value;
    private String type;
    private String ref;

    public GroupMember() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonGetter("$ref")
    public String getRef() {
        return ref;
    }

    @JsonSetter("$ref")
    public void setRef(String ref) {
        this.ref = ref;
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "value='" + value + '\'' +
                ", type='" + type + '\'' +
                ", ref='" + ref + '\'' +
                '}';
    }
}
