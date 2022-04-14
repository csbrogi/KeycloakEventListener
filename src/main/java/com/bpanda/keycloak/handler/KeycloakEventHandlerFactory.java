package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

public class KeycloakEventHandlerFactory {
    public static IKeycloakEventHandler create(ResourceType resourceType, OperationType operationType, KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String representation) {
        if (representation == null || keycloakData.getClientSecret() == null) {
            return new VoidEventHandler(resourceType, operationType, keycloakData.getRealmName());
        }String realmName = keycloakData.getRealmName();
        switch (resourceType) {
            case USER:
                switch (operationType) {
                    case CREATE:
                        return new UserCreatedHandler(kafkaAdapter, keycloakData, representation);
                    case UPDATE:
                        return new UserUpdatedHandler(kafkaAdapter, realmName, representation);
                    case DELETE:
                        return new UserDeletedHandler(kafkaAdapter, realmName, representation);
                }
                break;
            case GROUP:
                switch (operationType) {
                    case CREATE:
                        return new GroupCreatedHandler(kafkaAdapter, realmName, representation);
                    case UPDATE:
                        return new GroupUpdatedHandler(kafkaAdapter, realmName, representation);
                    case DELETE:
                        return new GroupDeletedHandler(kafkaAdapter, realmName, representation);
                }
                break;
            case REALM:
                if (operationType == OperationType.ACTION) {
                    return new RealmActionHandler(kafkaAdapter, realmName, representation);
                }
                break;
        }
        return null;
    }
}
