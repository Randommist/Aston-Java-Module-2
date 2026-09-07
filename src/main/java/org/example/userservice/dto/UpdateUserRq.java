package org.example.userservice.dto;

import jakarta.validation.constraints.*;

public record UpdateUserRq(
        @Positive(message = "Id должен быть больше 0")
        @NotNull(message = "Id должен быть заполнен")
        Long id,

        @NotBlank(message = "Имя не может быть пустым")
        String name,

        @NotBlank(message = "E-mail не может быть пустым")
        @Email(message = "Невалидный e-mail")
        String email,

        @Min(value = 0, message = "Возраст должен быть положительным")
        int age
) {
}
