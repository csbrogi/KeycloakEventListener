package com.bpanda.keycloak.model;

public class EnterpriseUser {
    private String division;
    private String organization;

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

}
