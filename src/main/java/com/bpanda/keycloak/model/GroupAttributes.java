package com.bpanda.keycloak.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

public class GroupAttributes {
    private List<String> externalId;
    private List<String> groupId;
    private List<String> ldapId;


    public GroupAttributes(List<String> externalId, List<String> groupId, List<String> ldapId) {
        this.externalId = externalId;
        this.groupId = groupId;
        this.ldapId = ldapId;
    }

    public GroupAttributes() {
    }


    public List<String> getExternalId() {
        return externalId;
    }

    public String getCAMPId() {
        if (groupId != null && groupId.size() == 1) {
            return groupId.get(0);
        }
        if (ldapId != null && ldapId.size() == 1) {
            return ldapId.get(0);
        }
        if (externalId != null && externalId.size() == 1) {
            return externalId.get(0);
        }
        return null;
    }
    public void setExternalId(List<String> externalId) {
        this.externalId = externalId;
    }

    @JsonGetter("GROUP_ID")
    public List<String> getGroupId() {
        return groupId;
    }

    @JsonSetter("GROUP_ID")
    public void setGroupId(List<String> groupId) {
        this.groupId = groupId;
    }

    @JsonGetter("LDAP_ID")
    public List<String> getLdapId() {
        return ldapId;
    }

    @JsonSetter("LDAP_ID")
    public void setLdapId(List<String> ldapId) {
        this.ldapId = ldapId;
    }
}
