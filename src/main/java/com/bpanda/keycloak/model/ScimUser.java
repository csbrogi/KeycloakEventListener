package com.bpanda.keycloak.model;


import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static com.bpanda.keycloak.model.EmailOrPhoneValue.getBestValue;

public class ScimUser {
    public static ScimUser getFromResource(String representation) {
        if (representation != null && !representation.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper().
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            try {
                return objectMapper.readValue(representation, ScimUser.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public ScimUser() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    private String userName;
    private String id;
    private String externalId;
    private String ldapId;
    private String title;

    private String displayName;
    private Name name;
    private List<EmailOrPhoneValue> emails;
    private List<EmailOrPhoneValue> phoneNumbers;
    private EnterpriseUser enterpriseUser;
    private ScimMetaData meta;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLdapId() {
        return ldapId;
    }

    public void setLdapId(String ldapId) {
        this.ldapId = ldapId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmail() {
        if (null != userName && userName.contains("@")) {
            return userName;
        }
        if (null != emails && !emails.isEmpty()) {
            return getBestValue(emails);
        }
        if (enterpriseUser != null) {
            return enterpriseUser.getEmail();
        }
        return null;
    }

    public List<EmailOrPhoneValue> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<EmailOrPhoneValue> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @JsonGetter("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User")
    public EnterpriseUser getEnterpriseUser() {
        return enterpriseUser;
    }
    @JsonSetter("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User")
    public void setEnterpriseUser(EnterpriseUser enterpriseUser) {
        this.enterpriseUser = enterpriseUser;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public List<EmailOrPhoneValue> getEmails() {
        return emails;
    }

    public void setEmails(List<EmailOrPhoneValue> emails) {
        this.emails = emails;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ScimMetaData getMeta() {
        return meta;
    }

    public void setMeta(ScimMetaData meta) {
        this.meta = meta;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + userName + '\'' +
                ", id='" + id + '\'' +
                ", externalId='" + externalId + '\'' +
                ", Title='" + title + '\'' +
                ", Display Name='" + displayName + '\'' +
                ", email='" + getEmail() + '\'' +
                '}';
    }

    public boolean isValid() {
        return null != userName && null != getEmail();
    }
}
