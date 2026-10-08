package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KeycloakUserCreatedHandlerTest {

    private static final String REALM = "myrealm";
    private static final String REPRESENTATION =
            "{\"username\":\"jdoe\",\"email\":\"jdoe@example.com\"}";

    private KafkaAdapter kafkaAdapter;
    private KeycloakSession session;
    private RealmModel realm;
    private UserProvider users;

    @BeforeEach
    void setUp() {
        kafkaAdapter = mock(KafkaAdapter.class);
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        users = mock(UserProvider.class);
        RealmProvider realms = mock(RealmProvider.class);
        when(session.realms()).thenReturn(realms);
        when(session.users()).thenReturn(users);
        when(realms.getRealm(REALM)).thenReturn(realm);
    }

    private KeycloakUserCreatedHandler handler(String representation) {
        return new KeycloakUserCreatedHandler(kafkaAdapter, KeycloakData.create("server", REALM), representation);
    }

    @Test
    void isValidReflectsRepresentation() {
        assertTrue(handler(REPRESENTATION).isValid());
        assertFalse(handler("{\"username\":\"jdoe\"}").isValid());
        assertFalse(handler("garbage").isValid());
    }

    @Test
    void stampsCreateTimestampAndSendsEvent() {
        UserModel user = mock(UserModel.class);
        when(user.getId()).thenReturn("user-id");
        when(users.getUserByEmail(realm, "jdoe@example.com")).thenReturn(user);

        handler(REPRESENTATION).handleRequest(session);

        verify(user).setSingleAttribute(eq("createTimestamp"), matches("\\d{12,}\\.0Z"));
        verify(kafkaAdapter).createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "user-id");
        verify(kafkaAdapter).send(eq(REALM), eq("users.added"),
                eq(EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_ADDED), any());
    }

    @Test
    void fallsBackToRealmByNameWhenRealmIdUnknown() {
        RealmProvider realms = session.realms();
        when(realms.getRealm(REALM)).thenReturn(null);
        when(realms.getRealmByName(REALM)).thenReturn(realm);
        UserModel user = mock(UserModel.class);
        when(user.getId()).thenReturn("user-id");
        when(users.getUserByEmail(realm, "jdoe@example.com")).thenReturn(user);

        handler(REPRESENTATION).handleRequest(session);

        verify(user).setSingleAttribute(eq("createTimestamp"), anyString());
    }

    @Test
    void invalidRepresentationSendsNothing() {
        handler("{\"username\":\"jdoe\"}").handleRequest(session);

        verifyNoInteractions(kafkaAdapter);
        verifyNoInteractions(users);
    }
}
