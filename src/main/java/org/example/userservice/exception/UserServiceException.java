package org.example.userservice.exception;

public class UserServiceException extends RuntimeException {

    private final String errorCode;

    public UserServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
