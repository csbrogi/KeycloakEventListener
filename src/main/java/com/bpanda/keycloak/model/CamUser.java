package com.bpanda.keycloak.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CamUser {
    private String id;
    private String userName;
    private String displayName;
    private String familyName;
    private String givenName;

    private String title;
    private String division;
    private String email;
    private String scimEmail;

    public CamUser(ScimUser scimUser) {
        id = scimUser.getId();
        userName = scimUser.getUserName();
        displayName = scimUser.getDisplayName();
        familyName = scimUser.getName().getFamilyName();
        givenName = scimUser.getName().getGivenName();
        title = scimUser.getTitle();
        email = scimUser.getEmail();
        if (null != scimUser.getEnterpriseUser()) {
            division = scimUser.getEnterpriseUser().getDivision();
        }
        scimEmail = email;
    }

    public String toJsonString() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getScimEmail() {
        return scimEmail;
    }

    public void setScimEmail(String scimEmail) {
        this.scimEmail = scimEmail;
    }
}
