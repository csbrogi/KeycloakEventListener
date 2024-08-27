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
import java.util.Map;
public class BpandaEventListenerProvider implements EventListenerProvider {

    private static final Logger log = LoggerFactory.getLogger(BpandaEventListenerProvider.class);

    private static final String DEFAULT_CLIENT_ID = "camp";

    private final KafkaAdapter kafkaAdapter;
    private final KeycloakSession keycloakSession;

    private final BpandaInfluxDBClient bpandaInfluxDBClient;
    private int eventCount = 0;

    public BpandaEventListenerProvider(String identityHost, String identityPort, KafkaProducer producer, BpandaInfluxDBClient bpandaInfluxDBClient, KeycloakSession keycloakSession) {
        this.kafkaAdapter = new KafkaAdapter(producer, identityHost, identityPort);
        this.keycloakSession = keycloakSession;
        this.bpandaInfluxDBClient = bpandaInfluxDBClient;
    }


    @Override
    public void onEvent(Event event) {
        log.info("KeycloakUserEvent:{}:{}", event.getType(), event.getClientId());
        eventCount++;
        String userId = event.getUserId();
        EventType eventType = event.getType();
        boolean handled = false;
        String realmName = event.getRealmId();
        RealmModel realm = keycloakSession.realms().getRealm(event.getRealmId());
        if (null != realm) {
            realmName = realm.getName();
        }
        if (null != bpandaInfluxDBClient) {
            if (event.getType().toString().endsWith("ERROR")) {
                bpandaInfluxDBClient.logError(event, realmName);
            } else {
                bpandaInfluxDBClient.logInfo(event.getId(), eventType.toString(), null, event.getTime(), realmName, event.getClientId());
            }
        }
        if (userId != null) {
            UserModel user = keycloakSession.users().getUserById(realm, userId);
            if (user != null) {
                switch (eventType) {
                    case RESET_PASSWORD:
                        user.setSingleAttribute("registered", "true");
                        handled = true;
                        break;
                    case UPDATE_PROFILE:
                    case CUSTOM_REQUIRED_ACTION:
                        user.setSingleAttribute("registered", "true");
                        EventMessages.AffectedElement affectedElement = EventMessages.AffectedElement.newBuilder()
                                .setElementType(EventMessages.ElementTypes.ELEMENT_USER_IDS)
                                .setValue(userId)
                                .build();
                        kafkaAdapter.send(realm.getId(), "users.updated", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_CHANGED, affectedElement);
                        handled = true;
                        break;
                    case LOGIN:
                        try {
                            setUserTimeStamp(user, "lastLoginTimestamp");
                            handled = true;
                        } catch (DateTimeException ex) {
                            log.error("setUserTimeStamp: ", ex);
                        }
                        break;
                    case LOGIN_ERROR:
                        try {
                            setUserTimeStamp(user, "lastLoginFailureTimestamp");
                            handled = true;
                        } catch (DateTimeException ex) {
                            log.error("setUserTimeStamp (lastLoginFailureTimestamp): ", ex);
                        }
                        break;
                    case REGISTER:
                        user.setSingleAttribute("registered", "true");
                        EventMessages.AffectedElement addedElement = EventMessages.AffectedElement.newBuilder()
                                .setElementType(EventMessages.ElementTypes.ELEMENT_USER_IDS)
                                .setValue(userId)
                                .build();
                        kafkaAdapter.send(realm.getId(), "users.added", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_ADDED, addedElement);
                        break;
                }
            }
        }
        log.info("Event Occurred: {} handled: {}", toString(event), handled);
    }


    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        eventCount++;
        if (!this.kafkaAdapter.isValid()) {
            log.error("kafka producer not set");
            return;
        }
        String realmId = adminEvent.getRealmId();
        RealmModel realm = keycloakSession.getContext().getRealm();
        String clientId = adminEvent.getAuthDetails().getClientId();
        if (null != realm && realm.getName() != null) {
            realmId = realm.getName();
            if (null != realm.getClientById(clientId)) {
                clientId = realm.getClientById(clientId).getClientId();
            }
        }

        String clientSecret = null;
        OperationType operationType = adminEvent.getOperationType();
        ResourceType resourceType = adminEvent.getResourceType();
        if (null != bpandaInfluxDBClient) {
            bpandaInfluxDBClient.logInfo(adminEvent.getId(), resourceType.toString(), operationType.toString(), adminEvent.getTime(), realmId, clientId);
        }
        try {
            if (resourceType == ResourceType.USER && null != clientId && null != realm) {
                ClientModel client = realm.getClientById(clientId);
                if (client == null) {
                    clientId = DEFAULT_CLIENT_ID;
                    client = realm.getClientByClientId(clientId);
                }
                if (null != client) {
                    clientSecret = client.getSecret();
                    log.info("RealmId: {}",realmId);
                }
            }

            String representation = adminEvent.getRepresentation();

            URI url = keycloakSession.getContext().getUri().getRequestUri();
            String protocol = url.getScheme();
            String authority = url.getAuthority();
            String keycloakServer = String.format("%s://%s", protocol, authority);
            KeycloakData keycloakData = KeycloakData.create(keycloakServer, realmId, clientSecret);
            IKeycloakEventHandler keycloakEventHandler = KeycloakEventHandlerFactory.create(resourceType, operationType, kafkaAdapter, keycloakData, representation, url);
            if (null != keycloakEventHandler && keycloakEventHandler.isValid()) {
                keycloakEventHandler.handleRequest(keycloakSession);
            }
            log.info("Admin Event Occurred:{}", toString(adminEvent));
            if (resourceType == ResourceType.GROUP_MEMBERSHIP) {
                // doesn't do anything
                log.info("Group membership Operation Type: {}:{}", representation, representation);
                Group group = Group.getFromResource(representation);
                if (group != null) {
                    String externalId = group.getId();

                    log.info("Group {} LDAP/id Id {} Operation {} ", group, externalId, operationType.toString());
                }
            }
            if (resourceType == ResourceType.REALM && bpandaInfluxDBClient != null ) {
                long realmCount = keycloakSession.realms().getRealmsStream().count();
                log.info("Realm Operation Type: {}:{} realmCount = {}", operationType, representation, realmCount);
                bpandaInfluxDBClient.logRealmCount(realmCount);
            }
        } catch (Exception ex) {
            log.error(String.format("onEvent resourceType %s operationType %s ", resourceType.toString(), operationType.toString()), ex);
        }
    }



    @Override
    public void close() {

    }
    private String toString(AdminEvent adminEvent) {
        return String.format("type=%s, realmId=%s", adminEvent.getResourceType(), adminEvent.getRealmId());
    }

    private void setUserTimeStamp(UserModel user, String timestampName) {
        try {
            user.setSingleAttribute(timestampName,  ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        } catch (DateTimeException ex) {
            log.error(String.format("setUserTimeStamp  %s: ", timestampName), ex);
        } catch (Exception e) {
            log.error(String.format("setUserTimeStamp  %s: Something went wrong", timestampName), e);
        }
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
