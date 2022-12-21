package com.bpanda.keycloak.handler;

import com.bpanda.keycloak.eventlistener.KafkaAdapter;
import com.bpanda.keycloak.model.KeycloakData;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

import java.net.URI;

public class KeycloakEventHandlerFactory {
    public final static String IDENTITY_PROVIDER = System.getenv("IDENTITY_PROVIDER");
    public static IKeycloakEventHandler create(ResourceType resourceType, OperationType operationType, KafkaAdapter kafkaAdapter, KeycloakData keycloakData, String representation, URI url) {
        if (operationType != OperationType.DELETE && representation == null || keycloakData.getClientSecret() == null) {
            return new VoidEventHandler(resourceType, operationType, keycloakData.getRealmName());
        }
        String realmName = keycloakData.getRealmName();
        switch (resourceType) {
            case USER:
                if ("keycloak".equalsIgnoreCase(IDENTITY_PROVIDER)) {
                    switch (operationType) {
                        case CREATE:
                            return new KeycloakUserCreatedHandler(kafkaAdapter, keycloakData, representation);
                        case UPDATE:
                            return new KeycloakUserUpdatedHandler(kafkaAdapter, realmName, representation);
                        case DELETE:
                            return new KeycloakUserDeletedHandler(kafkaAdapter, realmName, url, representation);
                    }}
                else {
                    switch (operationType) {
                        case CREATE:
                            return new UserCreatedHandler(kafkaAdapter, keycloakData, representation);
                        case UPDATE:
                            return new UserUpdatedHandler(kafkaAdapter, realmName, representation);
                        case DELETE:
                            return new UserDeletedHandler(kafkaAdapter, realmName, url, representation);
                    }
                }
                break;
            case GROUP:
                switch (operationType) {
                    case CREATE:
                        return new GroupCreatedHandler(kafkaAdapter, realmName, representation);
                    case UPDATE:
                        return new GroupUpdatedHandler(kafkaAdapter, realmName, representation);
                    case DELETE:
                        return new GroupDeletedHandler(kafkaAdapter, realmName, url, representation);
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
