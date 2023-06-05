package com.bpanda.keycloak.model;

import java.util.List;

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
        return getBestValue(emails);
    }

    public List<EmailOrPhoneValue> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<EmailOrPhoneValue> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public String getPhoneNumber() {
        return getBestValue(phoneNumbers);
    }

    private String getBestValue(List<EmailOrPhoneValue> values) {
        if(null != values && !values.isEmpty()) {
            if (values.size() == 1) {
                return values.get(0).getValue();
            }
            for (EmailOrPhoneValue p: values) {
                if (p.isPrimary()) {
                    return p.getValue();
                }
            }
            return values.get(0).getValue();
        }
        return null;
    }
}
