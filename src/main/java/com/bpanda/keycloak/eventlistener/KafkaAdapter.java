package com.bpanda.keycloak.eventlistener;

import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.keycloak.models.RealmModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public class KafkaAdapter {
    private static final Logger log = LoggerFactory.getLogger(KafkaAdapter.class);

    private final KafkaProducer producer;
    private static final String baseKafkaTopicNameKeycloak = "de.mid.keycloak.";
    private static final String baseKafkaTopicName = "de.mid.keycloak.realm.";
    private final String identityPort;
    private final String identityHost;

    // Events Datei wird im Verzeichnis
    // E:\bpanda-backend\modules\libs\event-protobuf\proto
    // mit dem Kommando
    // protoc --java_out=C:\Users\csbrogi\KeycloakEventListener\src\main\java  EventMessages.proto --proto_path=E:\bpanda-backend\modules\libs\wrapper-protobuf\proto;.
    // gebaut

    public KafkaAdapter(KafkaProducer producer, String identityHost, String identityPort) {
        this.producer = producer;
        this.identityHost = identityHost;
        this.identityPort = identityPort;

    }

    public void send(String realmName, String subTopic, EventMessages.EventTypes eventType, EventMessages.AffectedElement affectedElement) {
        if (producer != null) {
            EventMessages.AffectedElement realmData = createAffectedElement(EventMessages.ElementTypes.ELEMENT_REALM_NAME, realmName);

            EventMessages.Event.Builder builder = EventMessages.Event.newBuilder()
                    .setEventType(eventType)
                    .setTimestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .addData(realmData);

            if (affectedElement != null) {
                builder = builder.addData(affectedElement);
            }

            EventMessages.Event ev = builder.build();
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(baseKafkaTopicName + realmName, subTopic, ev.toByteArray());
            producer.send(record, (md, ex) -> {
                if (ex != null) {
                    System.err.println("exception occurred in producer for review :" + ev
                            + ", exception is " + ex);
                    ex.printStackTrace();
                } else {
                    System.out.println("Sent msg to " + md.partition() + " with offset " + md.offset() + " at " + md.timestamp());
                }
            });
            producer.flush();
        }
    }


    public void sendStatusUpdate(long realmCount, String allRealms) {
        if (identityPort == null || identityHost == null) {
            log.error("identityPort or identityHost not set - make sure IDENTITY_HOST and IDENTITY_PORT in Environment are set");
            return;
        }
        if (producer != null) {
            UUID uuid = UUID.nameUUIDFromBytes((this.identityHost + this.identityPort).getBytes());
            String keycloakId = uuid.toString();
            EventMessages.AffectedElement realmData = createAffectedElement(EventMessages.ElementTypes.ELEMENT_KEYCLOAK_REALMS, allRealms);
            EventMessages.AffectedElement affectedElement = EventMessages.AffectedElement.newBuilder()
                    .setElementType(EventMessages.ElementTypes.ELEMENT_KEYCLOAK_REALM_COUNT)
                    .setValue(Long.toString(realmCount))
                    .build();

            EventMessages.AffectedElement hostElement = EventMessages.AffectedElement.newBuilder()
                    .setElementType(EventMessages.ElementTypes.ELEMENT_KEYCLOAK_HOST)
                    .setValue(identityHost)
                    .build();

            EventMessages.AffectedElement portElement = EventMessages.AffectedElement.newBuilder()
                    .setElementType(EventMessages.ElementTypes.ELEMENT_KEYCLOAK_PORT)
                    .setValue(identityPort)
                    .build();

            EventMessages.Event.Builder builder = EventMessages.Event.newBuilder()
                    .setEventType(EventMessages.EventTypes.EVENT_KEYCLOAK_REALMS_INFO)
                    .setTimestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .addData(realmData)
                    .addData(hostElement)
                    .addData(portElement)
                    .addData(affectedElement);

            EventMessages.Event ev = builder.build();
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(baseKafkaTopicNameKeycloak + keycloakId, "realmsinfo", ev.toByteArray());
            producer.send(record, (md, ex) -> {
                if (ex != null) {
                    log.error("exception occurred in producer for review :" + ev
                            + ", exception is ", ex);
                    ex.printStackTrace();
                } else {
                    log.info("Sent msg to " + md.partition() + " with offset " + md.offset() + " at " + md.timestamp());
                }
            });
            producer.flush();
        }
    }

    public EventMessages.AffectedElement createAffectedElement(EventMessages.ElementTypes elementType, String value) {
        return EventMessages.AffectedElement.newBuilder()
                .setElementType(elementType)
                .setValue(value)
                .build();

    }

    public boolean isValid() {
        return producer != null;
    }
}
