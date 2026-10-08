package com.bpanda.keycloak.eventlistener;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class BpandaEventListenerProviderTest {

    private static final String REALM_ID = "realm-id";
    private static final String USER_ID = "user-id";

    private KafkaProducer producer;
    private BpandaInfluxDBClient influx;
    private KeycloakSession session;
    private RealmModel realm;
    private UserModel user;

    @BeforeEach
    void setUp() {
        producer = mock(KafkaProducer.class);
        influx = mock(BpandaInfluxDBClient.class);
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);

        RealmProvider realms = mock(RealmProvider.class);
        UserProvider users = mock(UserProvider.class);
        when(session.realms()).thenReturn(realms);
        when(session.users()).thenReturn(users);
        when(realms.getRealm(REALM_ID)).thenReturn(realm);
        when(realm.getId()).thenReturn(REALM_ID);
        when(users.getUserById(realm, USER_ID)).thenReturn(user);
    }

    private BpandaEventListenerProvider provider(KafkaProducer producer, BpandaInfluxDBClient influx) {
        return new BpandaEventListenerProvider("host", "8080", producer, influx, session, "kafka:9092",
                "LOGIN_ERROR,REFRESH_TOKEN_ERROR", "expired_code,cookie_not_found,session_expired");
    }

    private Event event(EventType type) {
        Event event = new Event();
        event.setId("evt-1");
        event.setType(type);
        event.setRealmId(REALM_ID);
        event.setUserId(USER_ID);
        event.setClientId("client");
        event.setTime(1234L);
        return event;
    }

    // --- user events ---

    @Test
    void loginStampsTimestampAndResetsFailureCount() {
        provider(producer, null).onEvent(event(EventType.LOGIN));

        verify(user).setSingleAttribute(eq("lastLoginTimestamp"), anyString());
        verify(user).setSingleAttribute("loginFailure", "0");
        verifyNoInteractions(producer);
    }

    @Test
    void loginErrorStampsFailureTimestamp() {
        provider(producer, null).onEvent(event(EventType.LOGIN_ERROR));

        verify(user).setSingleAttribute(eq("lastLoginFailureTimestamp"), anyString());
    }

    @Test
    void resetPasswordMarksUserRegistered() {
        provider(producer, null).onEvent(event(EventType.RESET_PASSWORD));

        verify(user).setSingleAttribute("registered", "true");
        verifyNoInteractions(producer);
    }

    @Test
    void updateProfileMarksUserRegisteredAndPublishesEvent() {
        provider(producer, null).onEvent(event(EventType.UPDATE_PROFILE));

        verify(user).setSingleAttribute("registered", "true");
        verify(producer).send(any(), any());
    }

    @Test
    void registerPublishesUsersAdded() {
        provider(producer, null).onEvent(event(EventType.REGISTER));

        verify(user).setSingleAttribute("registered", "true");
        verify(producer).send(any(), any());
    }

    @Test
    void eventWithoutUserIsOnlyCounted() {
        Event event = event(EventType.LOGIN);
        event.setUserId(null);

        provider(producer, null).onEvent(event);

        verifyNoInteractions(user);
        verifyNoInteractions(producer);
    }

    // --- InfluxDB logging of user events ---

    @Test
    void errorEventIsLoggedAsErrorToInflux() {
        Event event = event(EventType.CODE_TO_TOKEN_ERROR);
        event.setError("invalid_code");

        provider(producer, influx).onEvent(event);

        verify(influx).logError(event, true, REALM_ID);
    }

    @Test
    void ignoredErrorTypeIsNotFlaggedAsError() {
        Event event = event(EventType.LOGIN_ERROR);
        event.setError("invalid_user_credentials");

        provider(producer, influx).onEvent(event);

        verify(influx).logError(event, false, REALM_ID);
    }

    @Test
    void ignoredErrorCodeIsNotFlaggedAsError() {
        Event event = event(EventType.CODE_TO_TOKEN_ERROR);
        event.setError("expired_code");

        provider(producer, influx).onEvent(event);

        verify(influx).logError(event, false, REALM_ID);
    }

    @Test
    void regularEventIsLoggedAsInfoWithRealmName() {
        Event event = event(EventType.LOGIN);
        event.setDetails(null);
        // realmName falls back to realmId when the event carries none
        provider(producer, influx).onEvent(event);

        verify(influx).logInfo("evt-1", "LOGIN", null, 1234L, REALM_ID, "client");
    }

    // --- admin events ---

    private AdminEvent adminEvent(ResourceType resource, OperationType operation, String representation) {
        AdminEvent adminEvent = new AdminEvent();
        adminEvent.setId("adm-1");
        adminEvent.setRealmId(REALM_ID);
        adminEvent.setResourceType(resource);
        adminEvent.setOperationType(operation);
        adminEvent.setRepresentation(representation);
        adminEvent.setTime(99L);
        AuthDetails authDetails = new AuthDetails();
        authDetails.setClientId("admin-client");
        adminEvent.setAuthDetails(authDetails);
        return adminEvent;
    }

    private void stubContext() {
        KeycloakContext context = mock(KeycloakContext.class);
        KeycloakUriInfo uriInfo = mock(KeycloakUriInfo.class);
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        when(context.getUri()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri())
                .thenReturn(URI.create("http://localhost:8080/admin/realms/myrealm/groups/kc-g1"));
        when(realm.getName()).thenReturn("myrealm");
    }

    @Test
    void adminEventWithoutProducerIsIgnored() {
        provider(null, influx).onEvent(adminEvent(ResourceType.GROUP, OperationType.CREATE, "{}"), false);

        verifyNoInteractions(influx);
        verify(session, never()).getContext();
    }

    @Test
    void adminGroupCreateIsPublishedToRealmTopic() {
        stubContext();
        String group = "{\"id\":\"g1\",\"displayName\":\"Admins\"}";

        provider(producer, null).onEvent(adminEvent(ResourceType.GROUP, OperationType.CREATE, group), false);

        verify(producer).send(argThat(r -> ((org.apache.kafka.clients.producer.ProducerRecord) r).topic()
                .equals("de.mid.keycloak.realm.myrealm")), any());
    }

    @Test
    void adminEventWithoutErrorIsLoggedAsInfo() {
        stubContext();
        String group = "{\"id\":\"g1\",\"displayName\":\"Admins\"}";

        provider(producer, influx).onEvent(adminEvent(ResourceType.GROUP, OperationType.CREATE, group), false);

        verify(influx).logInfo("adm-1", "GROUP", "CREATE", 99L, "myrealm", "admin-client");
    }

    @Test
    void adminEventWithErrorIsLoggedAsError() {
        stubContext();
        AdminEvent adminEvent = adminEvent(ResourceType.GROUP, OperationType.CREATE, "{}");
        adminEvent.setError("something-failed");

        provider(producer, influx).onEvent(adminEvent, false);

        verify(influx).logError(adminEvent, "admin-client");
        verify(influx, never()).logInfo(anyString(), anyString(), anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void adminEventWithIgnoredErrorIsLoggedAsInfo() {
        stubContext();
        AdminEvent adminEvent = adminEvent(ResourceType.GROUP, OperationType.CREATE, "{}");
        adminEvent.setError("session_expired");

        provider(producer, influx).onEvent(adminEvent, false);

        verify(influx).logInfo("adm-1", "GROUP", "CREATE", 99L, "myrealm", "admin-client");
    }

    @Test
    void handlerFailuresDoNotPropagate() {
        // getContext().getUri() returns null -> NPE inside onEvent must be caught
        KeycloakContext context = mock(KeycloakContext.class);
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);

        provider(producer, null).onEvent(adminEvent(ResourceType.GROUP, OperationType.CREATE, "{}"), false);

        verifyNoInteractions(producer);
    }
}
