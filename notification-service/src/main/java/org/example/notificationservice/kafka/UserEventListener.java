package org.example.notificationservice.kafka;

import org.example.notificationservice.model.NotificationCommand;
import org.example.notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    private final NotificationService notificationService;

    public UserEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "user-lifecycle", groupId = "notification-service")
    public void receive(NotificationCommand command) {
        notificationService.send(command);
    }
}