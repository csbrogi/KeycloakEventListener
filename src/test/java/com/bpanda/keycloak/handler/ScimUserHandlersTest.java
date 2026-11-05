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

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScimUserHandlersTest {

    private static final String REALM = "myrealm";
    private static final URI USER_URI = URI.create("http://localhost/admin/realms/myrealm/users/kc-1");

    private KafkaAdapter kafka;
    private KeycloakSession session;
    private RealmModel realm;
    private UserProvider users;

    @BeforeEach
    void setUp() {
        kafka = mock(KafkaAdapter.class);
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        users = mock(UserProvider.class);
        RealmProvider realms = mock(RealmProvider.class);
        when(session.realms()).thenReturn(realms);
        when(session.users()).thenReturn(users);
        when(realms.getRealm(REALM)).thenReturn(realm);
    }

    // --- UserCreatedHandler ---

    @Test
    void createdHandlerSendsScimId() {
        UserCreatedHandler handler = new UserCreatedHandler(kafka, KeycloakData.create("server", REALM),
                "{\"id\":\"scim-1\",\"userName\":\"jdoe@example.com\"}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "scim-1");
        verify(kafka).send(eq(REALM), eq("users.added"),
                eq(EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_ADDED), any());
    }

    @Test
    void createdHandlerIsInvalidWithoutEmail() {
        UserCreatedHandler handler = new UserCreatedHandler(kafka, KeycloakData.create("server", REALM),
                "{\"id\":\"scim-1\",\"userName\":\"jdoe\"}");

        assertFalse(handler.isValid());
        handler.handleRequest(session);

        verifyNoInteractions(kafka);
    }

    // --- UserDeletedHandler ---

    @Test
    void deletedHandlerPrefersExternalId() {
        UserDeletedHandler handler = new UserDeletedHandler(kafka, REALM, USER_URI,
                "{\"externalId\":\"ext-1\",\"ldapId\":\"ldap-1\"}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "ext-1");
        verify(kafka).send(eq(REALM), eq("users.deleted"),
                eq(EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_DELETED), any());
    }

    @Test
    void deletedHandlerFallsBackToLdapId() {
        UserDeletedHandler handler = new UserDeletedHandler(kafka, REALM, USER_URI, "{\"ldapId\":\"ldap-1\"}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "ldap-1");
    }

    @Test
    void deletedHandlerIsInvalidWithoutRepresentation() {
        assertFalse(new UserDeletedHandler(kafka, REALM, USER_URI, null).isValid());
        assertFalse(new UserDeletedHandler(kafka, REALM, USER_URI, "{}").isValid());
    }

    // --- UserUpdatedHandler ---

    @Test
    void updatedHandlerStampsEnableTimestamp() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn("jdoe@example.com");
        when(users.getUserById(realm, "kc-1")).thenReturn(user);

        UserUpdatedHandler handler = new UserUpdatedHandler(kafka, REALM, USER_URI, "{\"enabled\":true}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(user).setSingleAttribute(eq("lastEnableTimestamp"), anyString());
    }

    @Test
    void updatedHandlerStampsDisableTimestamp() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn("jdoe@example.com");
        when(users.getUserById(realm, "kc-1")).thenReturn(user);

        new UserUpdatedHandler(kafka, REALM, USER_URI, "{\"enabled\":false}").handleRequest(session);

        verify(user).setSingleAttribute(eq("lastDisableTimestamp"), anyString());
    }

    @Test
    void updatedHandlerTreatsFullScimUserAsModification() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn("jdoe@example.com");
        when(users.getUserById(realm, "kc-1")).thenReturn(user);

        UserUpdatedHandler handler = new UserUpdatedHandler(kafka, REALM, USER_URI,
                "{\"id\":\"scim-1\",\"userName\":\"jdoe@example.com\"}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(user).setSingleAttribute(eq("lastModifiedTimestamp"), anyString());
        verify(user, never()).setSingleAttribute(eq("lastDisableTimestamp"), anyString());
        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "scim-1");
        verify(kafka).send(eq(REALM), eq("users.updated"),
                eq(EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_CHANGED), any());
    }

    @Test
    void updatedHandlerTreatsPatchOperationsAsModificationOfUriUser() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn("jdoe@example.com");
        when(users.getUserById(realm, "kc-1")).thenReturn(user);

        UserUpdatedHandler handler = new UserUpdatedHandler(kafka, REALM, USER_URI,
                "[{\"op\":\"replace\",\"path\":\"title\",\"value\":\"CEO\"}]");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(user).setSingleAttribute(eq("lastModifiedTimestamp"), anyString());
        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "kc-1");
    }

    @Test
    void updatedHandlerIsInvalidForPatchWithoutPath() {
        assertFalse(new UserUpdatedHandler(kafka, REALM, USER_URI,
                "[{\"op\":\"replace\",\"value\":{\"title\":\"CEO\"}}]").isValid());
    }

    @Test
    void updatedHandlerWithUnknownUserSendsNothing() {
        when(users.getUserById(realm, "kc-1")).thenReturn(null);

        new UserUpdatedHandler(kafka, REALM, USER_URI, "{\"enabled\":true}").handleRequest(session);

        verifyNoInteractions(kafka);
    }

    @Test
    void updatedHandlerIsInvalidForGarbage() {
        assertFalse(new UserUpdatedHandler(kafka, REALM, USER_URI, "garbage").isValid());
    }
}
