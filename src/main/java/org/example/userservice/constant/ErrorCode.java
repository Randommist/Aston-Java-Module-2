package org.example.userservice.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND("USER_NOT_FOUND", "Пользователь не найден"),
    VALIDATION_ERROR("VALIDATION_ERROR", "Ошибка валидации"),
    DB_ERROR("DB_ERROR", "Ошибка при работе с базой данных");

    private final String code;
    private final String message;


}