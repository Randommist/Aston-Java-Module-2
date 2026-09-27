package org.example.userservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.example.userservice.event.NotificationCommand;
import org.example.userservice.event.UserCreatedEvent;
import org.example.userservice.event.UserDeletedEvent;
import org.example.userservice.event.UserOperation;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class UserEventKafkaPublisher {

    static final String TOPIC = "user-lifecycle";

    private final KafkaTemplate<String, NotificationCommand> kafkaTemplate;

    public UserEventKafkaPublisher(KafkaTemplate<String, NotificationCommand> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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
        kafkaTemplate.send(TOPIC, command).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} event to {}", command.operation(), TOPIC, ex);
            } else {
                log.info("Published {} event to {}", command.operation(), TOPIC);
            }
        });
    }
}
