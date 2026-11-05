package com.bpanda.keycloak.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakUserTest {

    @Test
    void parsesKeycloakRepresentation() {
        KeycloakUser user = KeycloakUser.getFromResource(
                "{\"id\":\"1\",\"username\":\"jdoe\",\"firstName\":\"John\",\"lastName\":\"Doe\","
                        + "\"email\":\"jdoe@example.com\",\"attributes\":{\"dept\":[\"it\"]}}");

        assertNotNull(user);
        assertEquals("1", user.getId());
        assertEquals("jdoe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("jdoe@example.com", user.getEmail());
        assertEquals(List.of("it"), user.getAttributes().get("dept"));
        assertTrue(user.isValid());
    }

    @Test
    void ignoresUnknownProperties() {
        KeycloakUser user = KeycloakUser.getFromResource(
                "{\"username\":\"jdoe\",\"email\":\"jdoe@example.com\",\"enabled\":true,\"totp\":false}");

        assertNotNull(user);
        assertTrue(user.isValid());
    }

    @Test
    void invalidJsonReturnsNull() {
        assertNull(KeycloakUser.getFromResource("not json"));
    }

    @Test
    void userWithoutEmailIsInvalid() {
        KeycloakUser user = KeycloakUser.getFromResource("{\"username\":\"jdoe\"}");

        assertNotNull(user);
        assertFalse(user.isValid());
    }

    @Test
    void userWithoutUsernameIsInvalid() {
        KeycloakUser user = KeycloakUser.getFromResource("{\"email\":\"jdoe@example.com\"}");

        assertNotNull(user);
        assertFalse(user.isValid());
    }
}
