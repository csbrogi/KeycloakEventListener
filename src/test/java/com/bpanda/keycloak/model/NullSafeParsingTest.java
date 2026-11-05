package com.bpanda.keycloak.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NullSafeParsingTest {

    // --- EnableState ---

    @Test
    void enableStateParsesEnabledFlag() {
        assertTrue(EnableState.getFromResource("{\"enabled\":true}").isEnabled());
        assertFalse(EnableState.getFromResource("{\"enabled\":false}").isEnabled());
    }

    @Test
    void enableStateRequiresEnabledProperty() {
        assertNull(EnableState.getFromResource("{\"userName\":\"jdoe@example.com\"}"));
        assertNull(EnableState.getFromResource("{}"));
    }

    @Test
    void enableStateRejectsNonBooleanAndNonObjects() {
        assertNull(EnableState.getFromResource("{\"enabled\":\"yes\"}"));
        assertNull(EnableState.getFromResource("[{\"op\":\"replace\",\"path\":\"active\"}]"));
        assertNull(EnableState.getFromResource("garbage"));
    }

    @Test
    void enableStateHandlesMissingRepresentation() {
        assertNull(EnableState.getFromResource(null));
        assertNull(EnableState.getFromResource(""));
    }

    // --- ScimGroup ---

    @Test
    void scimGroupHandlesMissingRepresentation() {
        assertNull(ScimGroup.getFromResource(null));
        assertNull(ScimGroup.getFromResource(""));
    }

    @Test
    void scimGroupParsesRepresentation() {
        ScimGroup group = ScimGroup.getFromResource("{\"id\":\"g1\",\"displayName\":\"Admins\"}");

        assertNotNull(group);
        assertTrue(group.isValid());
    }

    // --- RealmAction ---

    @Test
    void realmActionHandlesMissingRepresentation() {
        assertNull(RealmAction.getFromResource(null));
        assertNull(RealmAction.getFromResource(""));
    }

    @Test
    void realmActionWithoutResultHasNoChangesAndPrintsSafely() {
        RealmAction action = RealmAction.getFromResource("{\"action\":\"triggerFullSync\"}");

        assertNotNull(action);
        assertFalse(action.hasChanges());
        assertDoesNotThrow(action::toString);
    }
}
