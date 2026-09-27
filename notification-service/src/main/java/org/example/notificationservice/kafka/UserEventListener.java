package org.example.notificationservice.kafka;

import org.example.notificationservice.model.NotificationCommand;
import org.example.notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class UserEventListener {

    private final JsonMapper jsonMapper;
    private final NotificationService notificationService;

    public UserEventListener(JsonMapper jsonMapper, NotificationService notificationService) {
        this.jsonMapper = jsonMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${notification.kafka.topic}")
    public void receive(String payload) {
        NotificationCommand command = jsonMapper.readValue(payload, NotificationCommand.class);
        notificationService.send(command);
    }
}
