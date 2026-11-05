package com.bpanda.keycloak.eventlistener;

import de.mid.smartfacts.bpm.dtos.event.v1.EventMessages;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class KafkaAdapterTest {

    @Test
    void isValidDependsOnProducer() {
        assertFalse(new KafkaAdapter(null, "host", "8080", "kafka:9092").isValid());
        assertTrue(new KafkaAdapter(mock(KafkaProducer.class), "host", "8080", "kafka:9092").isValid());
    }

    @Test
    void createAffectedElementSetsTypeAndValue() {
        KafkaAdapter adapter = new KafkaAdapter(null, "host", "8080", "kafka:9092");

        EventMessages.AffectedElement element =
                adapter.createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "42");

        assertEquals(EventMessages.ElementTypes.ELEMENT_USER_IDS, element.getElementType());
        assertEquals("42", element.getValue());
    }

    @Test
    void sendWithoutProducerDoesNothing() {
        KafkaAdapter adapter = new KafkaAdapter(null, "host", "8080", "kafka:9092");

        assertDoesNotThrow(() -> adapter.send("realm", "users.added",
                EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_ADDED, null));
    }

    @Test
    void sendPublishesEventToRealmTopic() throws Exception {
        KafkaProducer producer = mock(KafkaProducer.class);
        KafkaAdapter adapter = new KafkaAdapter(producer, "host", "8080", "kafka:9092");
        EventMessages.AffectedElement user =
                adapter.createAffectedElement(EventMessages.ElementTypes.ELEMENT_USER_IDS, "42");

        adapter.send("myrealm", "users.added", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_ADDED, user);

        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(captor.capture(), any());
        verify(producer).flush();

        ProducerRecord<String, byte[]> record = captor.getValue();
        assertEquals("de.mid.keycloak.realm.myrealm", record.topic());
        assertEquals("users.added", record.key());

        EventMessages.Event event = EventMessages.Event.parseFrom(record.value());
        assertEquals(EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_ADDED, event.getEventType());
        assertEquals(2, event.getDataCount());
        assertEquals(EventMessages.ElementTypes.ELEMENT_REALM_NAME, event.getData(0).getElementType());
        assertEquals("myrealm", event.getData(0).getValue());
        assertEquals(user, event.getData(1));
    }

    @Test
    void sendWithoutAffectedElementOnlyContainsRealmName() throws Exception {
        KafkaProducer producer = mock(KafkaProducer.class);
        KafkaAdapter adapter = new KafkaAdapter(producer, "host", "8080", "kafka:9092");

        adapter.send("myrealm", "realm.added", EventMessages.EventTypes.EVENT_KEYCLOAK_USERS_ADDED, null);

        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(captor.capture(), any());
        EventMessages.Event event = EventMessages.Event.parseFrom((byte[]) captor.getValue().value());
        assertEquals(1, event.getDataCount());
    }

    @Test
    void statusUpdateIsSkippedWithoutIdentityHostOrPort() {
        KafkaProducer producer = mock(KafkaProducer.class);

        new KafkaAdapter(producer, null, "8080", "kafka:9092").sendStatusUpdate(1, "a");
        new KafkaAdapter(producer, "host", null, "kafka:9092").sendStatusUpdate(1, "a");

        verifyNoInteractions(producer);
    }

    @Test
    void statusUpdateIsPublishedToKeycloakTopic() throws Exception {
        KafkaProducer producer = mock(KafkaProducer.class);
        KafkaAdapter adapter = new KafkaAdapter(producer, "host", "8080", "kafka:9092");

        adapter.sendStatusUpdate(3, "a,b,c");

        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(captor.capture(), any());
        verify(producer).flush();

        String expectedId = UUID.nameUUIDFromBytes("host8080".getBytes()).toString();
        assertEquals("de.mid.keycloak." + expectedId, captor.getValue().topic());
        assertEquals("realmsinfo", captor.getValue().key());

        EventMessages.Event event = EventMessages.Event.parseFrom((byte[]) captor.getValue().value());
        assertEquals(EventMessages.EventTypes.EVENT_KEYCLOAK_REALMS_INFO, event.getEventType());
        assertEquals(4, event.getDataCount());
        assertTrue(event.getDataList().stream().anyMatch(e ->
                e.getElementType() == EventMessages.ElementTypes.ELEMENT_KEYCLOAK_REALM_COUNT
                        && e.getValue().equals("3")));
    }
}
