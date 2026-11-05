package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KeycloakUserHandlersTest {

    private static final String REALM = "myrealm";

    private KafkaAdapter kafka;
    private KeycloakSession session;

    @BeforeEach
    void setUp() {
        kafka = mock(KafkaAdapter.class);
        session = mock(KeycloakSession.class);
    }

    // --- KeycloakUserUpdatedHandler ---

    @Test
    void updatedHandlerSendsUsersUpdated() {
        KeycloakUserUpdatedHandler handler = new KeycloakUserUpdatedHandler(kafka, REALM,
                "{\"id\":\"u1\",\"username\":\"jdoe\",\"email\":\"jdoe@example.com\"}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "u1");
        verify(kafka).send(eq(REALM), eq("users.updated"),
                eq(EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_CHANGED), any());
    }

    @Test
    void updatedHandlerIsInvalidWithoutEmail() {
        assertFalse(new KeycloakUserUpdatedHandler(kafka, REALM, "{\"username\":\"jdoe\"}").isValid());
        assertFalse(new KeycloakUserUpdatedHandler(kafka, REALM, "garbage").isValid());
    }

    // --- KeycloakUserDeletedHandler ---

    @Test
    void deletedHandlerTakesUserIdFromUri() {
        URI uri = URI.create("http://localhost/admin/realms/myrealm/users/u1");
        KeycloakUserDeletedHandler handler = new KeycloakUserDeletedHandler(kafka, REALM, uri, null);

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "u1");
        verify(kafka).send(eq(REALM), eq("users.deleted"),
                eq(EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_DELETED), any());
    }

    @Test
    void deletedHandlerIsInvalidWhenUriEndsWithSlash() {
        URI uri = URI.create("http://localhost/admin/realms/myrealm/users/");
        KeycloakUserDeletedHandler handler = new KeycloakUserDeletedHandler(kafka, REALM, uri, null);

        assertFalse(handler.isValid());
        handler.handleRequest(session);

        verifyNoInteractions(kafka);
    }
}
