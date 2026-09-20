package org.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRq(
        @NotBlank(message = "Имя не может быть пустым")
        String name,

        @NotBlank(message = "E-mail не может быть пустым")
        @Email(message = "Невалидный e-mail")
        String email,

        @Min(value = 0, message = "Возраст должен быть положительным")
        int age
) {
}
