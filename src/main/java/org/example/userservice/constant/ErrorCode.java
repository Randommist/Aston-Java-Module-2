package org.example.userservice.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND("USER_NOT_FOUND", "Пользователь не найден"),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "Пользователь с таким e-mail уже существует"),
    VALIDATION_ERROR("VALIDATION_ERROR", "Ошибка валидации"),
    DB_ERROR("DB_ERROR", "Ошибка при работе с базой данных");

    private final String code;
    private final String message;


}
