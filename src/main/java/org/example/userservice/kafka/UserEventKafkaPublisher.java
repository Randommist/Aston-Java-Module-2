package org.example.userservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.example.userservice.event.NotificationCommand;
import org.example.userservice.event.UserCreatedEvent;
import org.example.userservice.event.UserDeletedEvent;
import org.example.userservice.event.UserOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class UserEventKafkaPublisher {

    private final KafkaTemplate<String, NotificationCommand> kafkaTemplate;
    private final String topic;

    public UserEventKafkaPublisher(KafkaTemplate<String, NotificationCommand> kafkaTemplate,
                                   @Value("${user-service.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        send(new NotificationCommand(UserOperation.CREATED, event.email()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDeleted(UserDeletedEvent event) {
        send(new NotificationCommand(UserOperation.DELETED, event.email()));
    }

    private void send(NotificationCommand command) {
        kafkaTemplate.send(topic, command).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} event to {}", command.operation(), topic, ex);
            } else {
                log.info("Published {} event to {}", command.operation(), topic);
            }
        });
    }
}
