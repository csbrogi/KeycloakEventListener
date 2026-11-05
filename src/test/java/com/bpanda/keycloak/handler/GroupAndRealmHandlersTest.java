package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GroupAndRealmHandlersTest {

    private static final String REALM = "myrealm";
    private static final String GROUP = "{\"id\":\"g1\",\"displayName\":\"Admins\",\"members\":[{\"value\":\"u1\"}]}";
    private static final URI GROUP_URI = URI.create("http://localhost/admin/realms/myrealm/groups/kc-g1");

    private KafkaAdapter kafka;
    private KeycloakSession session;

    @BeforeEach
    void setUp() {
        kafka = mock(KafkaAdapter.class);
        session = mock(KeycloakSession.class);
    }

    // --- Group handlers ---

    @Test
    void groupCreatedSendsGroupId() {
        GroupCreatedHandler handler = new GroupCreatedHandler(kafka, REALM, GROUP);

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_GROUP_IDS, "g1");
        verify(kafka).send(eq(REALM), eq("groups.added"),
                eq(EventMessages.EventTypes.EVENT_KEYCLOAK_GROUPS_ADDED), any());
    }

    @Test
    void groupCreatedIsInvalidWithoutDisplayName() {
        assertFalse(new GroupCreatedHandler(kafka, REALM, "{\"id\":\"g1\"}").isValid());
        assertFalse(new GroupCreatedHandler(kafka, REALM, "garbage").isValid());
    }

    @Test
    void groupUpdatedSendsGroupChanged() {
        GroupUpdatedHandler handler = new GroupUpdatedHandler(kafka, REALM, GROUP);

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_GROUP_IDS, "g1");
        verify(kafka).send(eq(REALM), eq("groups.changed"),
                eq(EventMessages.EventTypes.EVENT_KEYCLOAK_GROUPS_CHANGED), any());
    }

    @Test
    void groupDeletedSendsDisplayName() {
        GroupDeletedHandler handler = new GroupDeletedHandler(kafka, REALM, GROUP_URI, GROUP);

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).createAffectedElement(EventMessages.ElementTypes.ELEMENT_GROUP_NAME, "Admins");
        verify(kafka).send(eq(REALM), eq("groups.deleted"),
                eq(EventMessages.EventTypes.EVENT_KEYCLOAK_GROUPS_DELETED), any());
    }

    @Test
    void groupDeletedIsInvalidWhenUriEndsWithSlash() {
        URI uri = URI.create("http://localhost/admin/realms/myrealm/groups/");

        assertFalse(new GroupDeletedHandler(kafka, REALM, uri, GROUP).isValid());
    }

    @Test
    void groupDeletedWithoutRepresentationIsValidButNotPublished() {
        // displayName is not set when the delete is triggered from the GUI
        GroupDeletedHandler handler = new GroupDeletedHandler(kafka, REALM, GROUP_URI, null);

        assertTrue(handler.isValid());
        assertDoesNotThrow(() -> handler.handleRequest(session));
        verifyNoInteractions(kafka);
    }

    @Test
    void groupDeletedWithoutDisplayNameIsValidButNotPublished() {
        GroupDeletedHandler handler = new GroupDeletedHandler(kafka, REALM, GROUP_URI, "{\"id\":\"g1\"}");

        assertTrue(handler.isValid());
        assertDoesNotThrow(() -> handler.handleRequest(session));
        verifyNoInteractions(kafka);
    }

    // --- RealmHandler ---

    @Test
    void realmHandlerSendsRealmCountAndNames() throws Exception {
        RealmProvider realms = mock(RealmProvider.class);
        RealmModel a = mock(RealmModel.class);
        RealmModel b = mock(RealmModel.class);
        when(a.getName()).thenReturn("a");
        when(b.getName()).thenReturn("b");
        when(session.realms()).thenReturn(realms);
        when(realms.getRealmsStream()).thenAnswer(inv -> Stream.of(a, b));

        RealmHandler handler = new RealmHandler(kafka, KeycloakData.create("http://kc", REALM), REALM,
                OperationType.CREATE, "{}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).sendStatusUpdate(2, "a,b");
    }

    @Test
    void realmHandlerSwallowsExceptions() {
        when(session.realms()).thenThrow(new IllegalStateException("boom"));

        RealmHandler handler = new RealmHandler(kafka, KeycloakData.create("http://kc", REALM), REALM,
                OperationType.DELETE, null);

        assertDoesNotThrow(() -> handler.handleRequest(session));
        verifyNoInteractions(kafka);
    }

    // --- RealmActionHandler ---

    @Test
    void realmActionWithChangesTriggersFullSyncEvent() throws Exception {
        RealmActionHandler handler = new RealmActionHandler(kafka, REALM,
                "{\"action\":\"triggerFullSync\",\"result\":{\"added\":1}}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verify(kafka).send(REALM, "users.synced", EventMessages.EventTypes.EVENT_KEYCLOAK_FULL_SYNC, null);
    }

    @Test
    void realmActionWithoutChangesSendsNothing() throws Exception {
        RealmActionHandler handler = new RealmActionHandler(kafka, REALM,
                "{\"action\":\"triggerChangedUsersSync\",\"result\":{\"status\":\"ok\"}}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verifyNoInteractions(kafka);
    }

    @Test
    void realmActionWithoutRepresentationIsInvalidAndDoesNotThrow() {
        RealmActionHandler handler = new RealmActionHandler(kafka, REALM, null);

        assertFalse(handler.isValid());
        assertDoesNotThrow(() -> handler.handleRequest(session));
        verifyNoInteractions(kafka);
    }

    @Test
    void realmActionWithoutActionIsInvalid() {
        RealmActionHandler handler = new RealmActionHandler(kafka, REALM, "{\"result\":{\"added\":1}}");

        assertFalse(handler.isValid());
        assertDoesNotThrow(() -> handler.handleRequest(session));
        verifyNoInteractions(kafka);
    }

    @Test
    void realmActionWithoutResultSendsNothing() throws Exception {
        RealmActionHandler handler = new RealmActionHandler(kafka, REALM, "{\"action\":\"triggerFullSync\"}");

        assertTrue(handler.isValid());
        handler.handleRequest(session);

        verifyNoInteractions(kafka);
    }

    @Test
    void realmActionWithUnknownActionIsInvalid() {
        RealmActionHandler handler = new RealmActionHandler(kafka, REALM,
                "{\"action\":\"somethingElse\",\"result\":{\"added\":1}}");

        assertFalse(handler.isValid());
    }
}
