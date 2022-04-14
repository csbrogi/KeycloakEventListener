package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

public class KeycloakEventHandlerFactory {
    public static IKeycloakEventHandler create(ResourceType resourceType, OperationType operationType, KafkaAdapter kafkaAdapter, String campServer, String accountId, KeycloakData keycloakData, String representation) {
        if (representation == null || keycloakData.getClientSecret() == null) {
            return new VoidEventHandler(resourceType, operationType, keycloakData.getRealmName());
        }
        if (resourceType == ResourceType.USER) {
            if (operationType == OperationType.CREATE) {
                return new UserCreatedHandler(kafkaAdapter, campServer, accountId, keycloakData, representation);
            }
            if (operationType == OperationType.UPDATE) {
                return new UserUpdatedHandler(kafkaAdapter, keycloakData, representation);
            }
        } else if (resourceType == ResourceType.GROUP) {
            if (operationType == OperationType.CREATE) {
                return new GroupCreatedHandler(kafkaAdapter, keycloakData, representation);
            }
            if (operationType == OperationType.UPDATE) {
                return new GroupUpdatedHandler(kafkaAdapter, keycloakData, representation);
            }
        } else if (resourceType == ResourceType.REALM) {
            if (operationType == OperationType.ACTION) {
                return new RealmActionHandler(kafkaAdapter, campServer, accountId, keycloakData, representation);
            }
        }
        return null;
    }
}
