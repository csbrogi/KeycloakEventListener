package com.bpanda.keycloak.eventlistener;

import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Instant;

public class KafkaAdapter {
    private final KafkaProducer producer;
    private final String baseKafkaTopicName = "de.mid.keycloak.realm.";

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

            EventMessages.Event ev = EventMessages.Event.newBuilder()
                    .setEventType(eventType)
                    .setTimestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .addData(affectedElement)
                    .addData(realmData)
                    .build();
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(baseKafkaTopicName + realmName, subTopic, ev.toByteArray());
            producer.send(record, (md, ex) -> {
                if (ex != null) {
                    System.err.println("exception occurred in producer for review :" + ev
                            + ", exception is " + ex);
                    ex.printStackTrace();
                } else {
                    System.err.println("Sent msg to " + md.partition() + " with offset " + md.offset() + " at " + md.timestamp());
                }
                producer.flush();
            });
        }
    }

//    public void send(EventMessages.Event ev, String realmName, String subTopic) {
//        if (producer != null) {
//            ProducerRecord<String, byte[]> record = new ProducerRecord<>(baseKafkaTopicName + realmName, subTopic,  ev.toByteArray());
//            producer.send(record, (md, ex) -> {
//                if (ex != null) {
//                    System.err.println("exception occurred in producer for review :" + ev
//                            + ", exception is " + ex);
//                    ex.printStackTrace();
//                } else {
//                    System.err.println("Sent msg to " + md.partition() + " with offset " + md.offset() + " at " + md.timestamp());
//                }
//                producer.flush();
//            });
//        }
//    }

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
