package com.bpanda.keycloak.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Group {
    private String id;
    private String name;
    private GroupAttributes attributes;
    private List<GroupMember> members;

    public static Group getFromResource(String representation) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            Group group = objectMapper.readValue(representation, Group.class);
//            objectMapper.read
            return group;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Group() {
        members = new ArrayList<>();
    }

    public String getCAMPId() {
        String ret = null;
        if (attributes != null) {
            ret = attributes.getCAMPId();
        }
        if (ret == null) {
            ret = getId();
        }
        return ret;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GroupAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(GroupAttributes attributes) {
        this.attributes = attributes;
    }

    public List<GroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMember> members) {
        this.members = members;
    }
}
