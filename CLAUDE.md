# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Full build (creates jar-with-dependencies)
mvn clean package

# Compile only
mvn compile

# Run tests (none currently defined)
mvn test
```

**Output artifact:** `target/keycloak-event-listener-jar-with-dependencies.jar`
Deploy by copying to Keycloak's `standalone/deployments/` directory.

**Protobuf regeneration** (when proto files change — sourced from an external repo):
```bash
protoc -I=<path>/wrapper-protobuf/proto -I=<path>/event-protobuf/proto \
  --java_out=src/main/java \
  <path>/event-protobuf/proto/EventMessages.proto
```
The generated files (`EventMessages.java`, `OptionWrapper.java`) live under `src/main/java/de/mid/smartfacts/bpm/dtos/`.

## Architecture Overview

This is a **Keycloak SPI plugin** that intercepts Keycloak authentication and admin events and forwards them to downstream systems (Kafka, InfluxDB).

### Event Flow

1. Keycloak fires a user or admin event
2. `BpandaEventListenerProvider` receives it via `onEvent()`
3. Login/register/logout events update user attributes (timestamps) directly on the Keycloak session
4. Admin events (USER, GROUP, GROUP_MEMBERSHIP, REALM resources) are routed to a handler via `KeycloakEventHandlerFactory`
5. The handler deserializes the event JSON representation into a model object, serializes it to Protobuf, and sends it to Kafka topic `de.mid.keycloak.realm.{realmName}`
6. Errors and metrics are optionally written to InfluxDB asynchronously

### Key Classes

| Class | Role |
|---|---|
| `BpandaEventListenerProviderFactory` | Factory (Keycloak SPI entry point); initializes Kafka producer, InfluxDB client, and a scheduled status-update task |
| `BpandaEventListenerProvider` | Per-session listener; handles event routing and user-attribute stamping |
| `KeycloakEventHandlerFactory` | Creates the correct handler for each admin event resource+operation; checks `EVENT_SOURCE` env var to pick Keycloak-native vs. SCIM handlers |
| `KafkaAdapter` | Serializes models to Protobuf and publishes to Kafka |
| `BpandaInfluxDBClient` | Async writes of error/metric measurements to InfluxDB |
| `handler/*` | One handler class per resource × operation combination (UserCreated, GroupDeleted, etc.) |
| `model/*` | POJOs deserialized from Keycloak admin event representation JSON |

### Dual-Mode Handler Routing

The `EVENT_SOURCE` environment variable controls which handler set is used:
- `"Keycloak"` → `KeycloakUser*Handler` classes (native Keycloak user representation)
- anything else → `User*Handler` / `Group*Handler` classes (SCIM protocol representation)

### Scheduled Status Updates

`BpandaEventListenerProviderFactory.postInit()` registers a Keycloak timer task that fires every `IDENTITY_UPDATE_TIMER` seconds (default 12). It sends a realm-count status message to Kafka (every 5th tick) and to InfluxDB (every 20th tick).

## Configuration (Environment Variables)

| Variable | Default | Purpose |
|---|---|---|
| `KAFKA_HOST` / `KAFKA_PORT` | — | Kafka broker; if absent, Kafka publishing is disabled |
| `IDENTITY_HOST` / `IDENTITY_PORT` | — | This Keycloak instance's address (included in status messages) |
| `IDENTITY_UPDATE_TIMER` | `12` | Status-update interval in seconds |
| `EVENT_SOURCE` | — | `"Keycloak"` for native handlers; anything else for SCIM handlers |
| `KEYCLOAK_IGNORED_ERROR_TYPES` | `LOGIN_ERROR,REFRESH_TOKEN_ERROR` | Event types not forwarded to Kafka |
| `KEYCLOAK_IGNORED_ERRORS` | `expired_code,cookie_not_found,session_expired` | Error codes not forwarded |
| `MONITORING_ENVIRONMENT_NAME` | — | **Enables InfluxDB** when set; used as the database name |
| `MONITORING_INFLUXDB_HOST` | `marvin.mid.de` | InfluxDB host |
| `MONITORING_INFLUXDB_PORT` | `8086` | InfluxDB port |
| `MONITORING_INFLUXDB_SECRET` | — | InfluxDB password |
| `INFLUXDB_USER` | `smartfacts-monitoring-client` | InfluxDB username |
| `INFLUXDB_DB_RETENTION_POLICY` | `` | InfluxDB retention policy |

## Dependencies & Versions

- **Keycloak SPI**: `26.5.6` (provided scope — must match the target Keycloak version)
- **Kafka client**: `4.1.0`
- **InfluxDB Java client**: `2.25`
- **Protobuf**: `3.25.6`
- **Java**: 11 (compile target)
