package com.bpanda.keycloak.eventlistener;

import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.stream.Collectors;

public class KafkaAdapter {
    private static final Logger log = LoggerFactory.getLogger(KafkaAdapter.class);

    private final KafkaProducer producer;
    private static final String baseKafkaTopicNameKeycloak = "de.mid.keycloak.";
    private static final String baseKafkaTopicName = "de.mid.keycloak.realm.";

    // Events Datei wird im Verzeichnis
    // E:\bpanda-backend\modules\libs\event-protobuf\proto
    // mit dem Kommando
    // protoc --java_out=C:\Users\csbrogi\KeycloakEventListener\src\main\java  EventMessages.proto --proto_path=E:\bpanda-backend\modules\libs\wrapper-protobuf\proto;.
    // gebaut

    public KafkaAdapter(KafkaProducer producer) {
        this.producer = producer;
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
                    log.error(String.format("exception occurred in producer for review :%s, exception is ", ev), ex);
                } else {
                    log.info(String.format("Sent msg to %d with offset %d at %d", md.partition(), md.offset(), md.timestamp()));
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
