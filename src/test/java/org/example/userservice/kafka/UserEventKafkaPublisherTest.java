package org.example.userservice.kafka;

import org.example.userservice.event.NotificationCommand;
import org.example.userservice.event.UserCreatedEvent;
import org.example.userservice.event.UserDeletedEvent;
import org.example.userservice.event.UserOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventKafkaPublisherTest {

    private static final String TOPIC = "user-lifecycle";
    private static final String EMAIL = "test@example.com";

    @Mock
    private KafkaTemplate<String, NotificationCommand> kafkaTemplate;

    private UserEventKafkaPublisher publisher;

    private final CompletableFuture<SendResult<String, NotificationCommand>> sendFuture = new CompletableFuture<>();

    @BeforeEach
    void setUp() {
        publisher = new UserEventKafkaPublisher(kafkaTemplate, TOPIC);
        when(kafkaTemplate.send(anyString(), any(NotificationCommand.class))).thenReturn(sendFuture);
    }

    @Test
    void onUserCreated_sendsCreatedEventToKafka() {
        UserCreatedEvent event = new UserCreatedEvent(EMAIL);

        publisher.onUserCreated(event);

        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(kafkaTemplate).send(eq(TOPIC), captor.capture());

        NotificationCommand sent = captor.getValue();
        assertEquals(EMAIL, sent.email());
        assertEquals(UserOperation.CREATED, sent.operation());
    }

    @Test
    void onUserDeleted_sendsDeletedEventToKafka() {
        UserDeletedEvent event = new UserDeletedEvent(EMAIL);

        publisher.onUserDeleted(event);

        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(kafkaTemplate).send(eq(TOPIC), captor.capture());

        NotificationCommand sent = captor.getValue();
        assertEquals(EMAIL, sent.email());
        assertEquals(UserOperation.DELETED, sent.operation());
    }

    @Test
    void onUserCreated_sendsToCorrectTopic() {
        publisher.onUserCreated(new UserCreatedEvent(EMAIL));

        verify(kafkaTemplate).send(eq("user-lifecycle"), any(NotificationCommand.class));
    }

    @Test
    void onUserDeleted_sendsToCorrectTopic() {
        publisher.onUserDeleted(new UserDeletedEvent(EMAIL));

        verify(kafkaTemplate).send(eq("user-lifecycle"), any(NotificationCommand.class));
    }

    @Test
    void onUserCreated_doesNotSendMoreThanOnce() {
        publisher.onUserCreated(new UserCreatedEvent(EMAIL));

        verify(kafkaTemplate, times(1)).send(anyString(), any(NotificationCommand.class));
    }

    @Test
    void onUserCreated_sendFailure_doesNotThrow() {
        publisher.onUserCreated(new UserCreatedEvent(EMAIL));

        assertDoesNotThrow(() -> sendFuture.completeExceptionally(new RuntimeException("broker down")));
    }
}
