package org.example.userservice.exception;

import lombok.Getter;
import org.example.userservice.constant.ErrorCode;

@Getter
public class UserServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public UserServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}