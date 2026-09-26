package org.example.notificationservice.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.example.notificationservice.model.NotificationCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final Validator validator;
    private final String senderAddress;

    public NotificationService(JavaMailSender mailSender,
                               Validator validator,
                               @Value("${notification.mail.from}") String senderAddress) {
        this.mailSender = mailSender;
        this.validator = validator;
        this.senderAddress = senderAddress;
    }

    public void send(NotificationCommand command) {
        Set<ConstraintViolation<NotificationCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(command.email());

        switch (command.operation()) {
            case CREATED -> {
                message.setSubject("Аккаунт создан");
                message.setText("Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.");
            }
            case DELETED -> {
                message.setSubject("Аккаунт удалён");
                message.setText("Здравствуйте! Ваш аккаунт был удалён.");
            }
        }

        mailSender.send(message);
    }
}
