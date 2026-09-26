package org.example.notificationservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationCommand(
        @NotNull UserOperation operation,
        @NotBlank @Email String email
) {
}
