package com.bpanda.keycloak.eventlistener;

import com.bpanda.keycloak.handler.IKeycloakEventHandler;
import com.bpanda.keycloak.handler.KeycloakEventHandlerFactory;
import com.bpanda.keycloak.model.Group;
import com.bpanda.keycloak.model.KeycloakData;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class BpandaEventListenerProvider implements EventListenerProvider {

    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);
    private static final String LDAP_ID = "LDAP_ID";
    private static final String DEFAULT_CLIENT_ID = "camp";

    private final KafkaAdapter kafkaAdapter;
    private final KeycloakSession keycloakSession;
    private final String campServer;
    private String accountId;
    private int eventCount = 0;

    public BpandaEventListenerProvider(KafkaProducer producer, String campServer, KeycloakSession keycloakSession) {
        this.kafkaAdapter = new KafkaAdapter(producer);
        this.campServer = campServer;
        this.keycloakSession = keycloakSession;
    }

    @Override
    public void onEvent(Event event) {
        log.info("KeycloakUserEvent:" + event.getType() + ":" + event.getClientId());
        eventCount++;
        String userId = event.getUserId();
        EventType eventType = event.getType();
        if (userId != null) {
            RealmModel realm = keycloakSession.realms().getRealm(event.getRealmId());
            UserModel user = keycloakSession.users().getUserById(userId, realm);
            if (user != null) {
                switch (eventType) {
                    case RESET_PASSWORD:
                        user.setSingleAttribute("registered", "true");
                        break;
                    case UPDATE_PROFILE:
                    case CUSTOM_REQUIRED_ACTION:
                        user.setSingleAttribute("registered", "true");
                        EventMessages.AffectedElement affectedElement = EventMessages.AffectedElement.newBuilder()
                                .setElementType(EventMessages.ElementTypes.ELEMENT_USER_IDS)
                                .setValue(userId)
                                .build();

                        kafkaAdapter.send(realm.getId(), "users.updated", EventMessages.EventTypes.EVENT_CAM_USERS_CHANGED, affectedElement );
                        break;
                    case LOGIN:
                        try {
                            setUserTimeStamp(user, "lastLoginTimestamp");
                        } catch (DateTimeException ex)  {
                            ex.printStackTrace();
                        }
                        break;
                    case LOGIN_ERROR:
                        try {
                            setUserTimeStamp(user, "lastLoginFailureTimestamp");
                        } catch (DateTimeException ex)  {
                            ex.printStackTrace();
                        }
                        break;
                }
            }
        }
        System.out.println("Event Occurred:" + toString(event));
    }


    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        eventCount++;
        if (null == this.campServer && !this.kafkaAdapter.isValid()) {
            log.error("camp server and produce not set");
            return;
        }
        String realmId = adminEvent.getRealmId();
        RealmModel realm = keycloakSession.getContext().getRealm();
        if (null != realm && realm.getName() != null) {
            realmId = realm.getName();
        }

        String clientId = adminEvent.getAuthDetails().getClientId();
        String clientSecret = null;

        if (null != clientId && null != realm) {
            ClientModel client = realm.getClientById(clientId);
//            TokenManager tokenManager = keycloakSession.tokens();
            if (client == null) {
                clientId = DEFAULT_CLIENT_ID;
                client = realm.getClientByClientId(clientId);
            }
            if (null != client) {
                clientSecret = client.getSecret();
                String desc = client.getDescription();
                if (null != desc && desc.length() > 0) {
                    accountId = desc;
                }
                if (null != client.getClientId()) {
                    clientId = client.getClientId();
                }
                log.info(("Description" + " " + desc + " RealmId: " + realmId));
            }
        }
        try {
            OperationType operationType = adminEvent.getOperationType();
            ResourceType resourceType = adminEvent.getResourceType();
            String represantation = adminEvent.getRepresentation();
            log.error("KeycloakAdminEvent:" + resourceType + ":" + adminEvent.getRealmId());

            URI url = keycloakSession.getContext().getUri().getRequestUri();
            String protocol = url.getScheme();
            String authority = url.getAuthority();
            String keycloakServer = String.format("%s://%s", protocol, authority);
            KeycloakData keycloakData = KeycloakData.create(keycloakServer, realmId, clientId, clientSecret);
            IKeycloakEventHandler keycloakEventHandler = KeycloakEventHandlerFactory.create(resourceType, operationType, kafkaAdapter, campServer, accountId, keycloakData, represantation);
            if (null != keycloakEventHandler && keycloakEventHandler.isValid()) {
                keycloakEventHandler.handleRequest(keycloakSession);
                return;
            }
            if (resourceType == ResourceType.GROUP_MEMBERSHIP) {
                log.error("Groupmembership Operation Type: " + represantation + ":" + represantation);
                Group group = Group.getFromResource(represantation);
                if (group != null) {
                    String externalId = group.getId();
                    Object attributesObject = group.getAttributes();
                    if (attributesObject != null) {
                        HashMap<String, List<String>> attributes = (HashMap) attributesObject;
                        if (attributes.containsKey(LDAP_ID)) {
                            List<String> values = attributes.get(LDAP_ID);
                            if (values != null && !values.isEmpty()) {
                                externalId = values.get(0);
                            }
                        }
                    }
                    log.info(String.format("Group %s LDAP/id Id %s Operation %s ", group, externalId, operationType.toString()));
                }
            }
            System.err.println("Admin Event Occurred:" + toString(adminEvent));
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("onEvent " + ex);
        }
    }

    @Override
    public void close() {
        log.info("close events: " + eventCount);
    }
    private String toString(AdminEvent adminEvent) {
        return  "type=" +
                adminEvent.getResourceType() +
                ", realmId=" +
                adminEvent.getRealmId();
    }

    private void setUserTimeStamp(UserModel user, String timestampName) {
        user.setSingleAttribute(timestampName,  ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
    }

    private String toString(Event event) {
        StringBuilder sb = new StringBuilder();
        sb.append("type=");
        sb.append(event.getType());
        sb.append(", realmId=");
        sb.append(event.getRealmId());
        sb.append(", clientId=");
        sb.append(event.getClientId());
        sb.append(", userId=");
        sb.append(event.getUserId());
        sb.append(", ipAddress=");
        sb.append(event.getIpAddress());
        if (event.getError() != null) {
            sb.append(", error=");
            sb.append(event.getError());
        }

        if (event.getDetails() != null) {
            for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                sb.append(", ");
                sb.append(e.getKey());
                if (e.getValue() == null || e.getValue().indexOf(' ') == -1) {
                    sb.append("=");
                    sb.append(e.getValue());
                } else {
                    sb.append("='");
                    sb.append(e.getValue());
                    sb.append("'");
                }
            }
        }
        return sb.toString();
    }
}
