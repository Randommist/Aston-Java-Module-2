package org.example.userservice.kafka;

import org.example.userservice.event.NotificationCommand;
import org.example.userservice.event.UserCreatedEvent;
import org.example.userservice.event.UserDeletedEvent;
import org.example.userservice.event.UserOperation;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserEventKafkaPublisher {

    private final KafkaTemplate<String, NotificationCommand> kafkaTemplate;

    public UserEventKafkaPublisher(KafkaTemplate<String, NotificationCommand> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        kafkaTemplate.send("user-lifecycle",
                new NotificationCommand(UserOperation.CREATED, event.email()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDeleted(UserDeletedEvent event) {
        kafkaTemplate.send("user-lifecycle",
                new NotificationCommand(UserOperation.DELETED, event.email()));
    }
}