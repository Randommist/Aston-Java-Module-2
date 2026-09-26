package org.example.userservice.event;

public record NotificationCommand(String email, UserOperation operation) {}