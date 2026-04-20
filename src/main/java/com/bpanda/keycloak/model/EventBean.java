package com.bpanda.keycloak.model;

import org.keycloak.events.Event;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventBean {
    private final Event event;
    private final String email;
    private final int loginFailureCount;

    public EventBean(Event event, String email, int loginFailureCount) {
        this.event = event;
        this.email = email;
        this.loginFailureCount = loginFailureCount;
    }

    public Date getDate() {
        return new Date(event.getTime());
    }

    public String getEvent() {
        return event.getType().toString().toLowerCase().replace("_", " ");
    }

    public String getClient() {
        return event.getClientId();
    }

    /**
     * Note: will not be an address when a proxy does not provide a valid one
     *
     * @return the ip address
     */
    public String getIpAddress() {
        return event.getIpAddress();
    }

    public String getRealm() {
        return event.getRealmId();
    }
    public String getEmail() {
        return email;
    }
    public String getLoginFailureCount() {
        return  String.valueOf(loginFailureCount);
    }
    public List<DetailBean> getDetails() {
        List<DetailBean> details = new LinkedList<DetailBean>();
        for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
            details.add(new DetailBean(e));
        }
        return details;
    }

    public String getDetail(String name) {
        return event.getDetails() != null
                ? event.getDetails().get(name)
                : null;
    }

    public static class DetailBean {

        private Map.Entry<String, String> entry;

        public DetailBean(Map.Entry<String, String> entry) {
            this.entry = entry;
        }

        public String getKey() {
            return entry.getKey();
        }

        public String getValue() {
            return entry.getValue().replace("_", " ");
        }

    }
}
