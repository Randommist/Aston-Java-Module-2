package org.example.userservice.event;

public record NotificationCommand(UserOperation operation, String email) {}