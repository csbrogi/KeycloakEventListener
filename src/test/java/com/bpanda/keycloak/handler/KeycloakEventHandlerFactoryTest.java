package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import org.junit.jupiter.api.Test;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * EVENT_SOURCE is read once from the environment, so only the paths that do not
 * depend on it are covered here.
 */
class KeycloakEventHandlerFactoryTest {

    private final KafkaAdapter kafka = mock(KafkaAdapter.class);
    private final KeycloakData data = KeycloakData.create("server", "myrealm");
    private final URI url = URI.create("http://localhost/admin/realms/myrealm/groups/1");

    private IKeycloakEventHandler create(ResourceType resource, OperationType operation, String representation) {
        return KeycloakEventHandlerFactory.create(resource, operation, kafka, data, representation, url);
    }

    @Test
    void missingRepresentationYieldsVoidHandler() {
        IKeycloakEventHandler handler = create(ResourceType.USER, OperationType.CREATE, null);

        assertInstanceOf(VoidEventHandler.class, handler);
        assertTrue(handler.isValid());
    }

    @Test
    void groupOperationsAreMappedToGroupHandlers() {
        String group = "{\"id\":\"1\",\"displayName\":\"g\"}";

        assertInstanceOf(GroupCreatedHandler.class, create(ResourceType.GROUP, OperationType.CREATE, group));
        assertInstanceOf(GroupUpdatedHandler.class, create(ResourceType.GROUP, OperationType.UPDATE, group));
        assertInstanceOf(GroupDeletedHandler.class, create(ResourceType.GROUP, OperationType.DELETE, group));
    }

    @Test
    void unsupportedResourceReturnsNull() {
        assertNull(create(ResourceType.CLIENT, OperationType.CREATE, "{}"));
    }

    @Test
    void unsupportedRealmOperationReturnsNull() {
        assertNull(create(ResourceType.REALM, OperationType.UPDATE, "{}"));
    }
}
