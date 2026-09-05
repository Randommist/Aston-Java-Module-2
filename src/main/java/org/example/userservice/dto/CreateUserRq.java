package org.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRq(
        @NotBlank(message = "Имя не должно быть пустым")
        String name,

        @NotBlank(message = "E-mail должен быть заполнен")
        @Email(message = "Невалидный имейл")
        String email,

        @Min(value = 0, message = "Возраст должен быть положительным")
        int age
) {
}
