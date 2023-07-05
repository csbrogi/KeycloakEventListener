package com.bpanda.keycloak.model;

import java.util.List;

import static com.bpanda.keycloak.model.EmailOrPhoneValue.getBestValue;

public class EnterpriseUser {
    private String division;
    private String organization;

    private List<EmailOrPhoneValue> emails;

    private List<EmailOrPhoneValue> phoneNumbers;

    public EnterpriseUser() {
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
    public List<EmailOrPhoneValue> getEmails() {
        return emails;
    }

    public void setEmails(List<EmailOrPhoneValue> emails) {
        this.emails = emails;
    }

    public String getEmail() {
        if (null != emails && !emails.isEmpty()) {
            return getBestValue(emails);
        }
        return null;
    }

    public List<EmailOrPhoneValue> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<EmailOrPhoneValue> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public String getPhoneNumber() {
        if (null != phoneNumbers && !phoneNumbers.isEmpty()) {
            return getBestValue(phoneNumbers);
        }
        return null;
    }

}
