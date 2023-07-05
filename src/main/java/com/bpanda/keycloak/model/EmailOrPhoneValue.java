package com.bpanda.keycloak.model;

import java.util.List;

public class EmailOrPhoneValue {
    private String value;
    private String type;
    private boolean primary;

    public EmailOrPhoneValue() {
    }

    public static String getBestValue(List<EmailOrPhoneValue> values) {
        if (values.size() == 1) {
            return values.get(0).getValue();
        }
        for (EmailOrPhoneValue e : values) {
            if (e.isPrimary()) {
                return e.getValue();
            }
        }
        return values.get(0).getValue();
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

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
