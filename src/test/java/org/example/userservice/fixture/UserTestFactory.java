package org.example.userservice.fixture;

import org.example.userservice.database.entity.User;

public final class UserTestFactory {

    public static final String DEFAULT_USER_NAME = "John";
    public static final String DEFAULT_USER_EMAIL = "john@example.com";
    public static final int DEFAULT_USER_AGE = 30;

    public static final String OTHER_USER_NAME = "Jane";
    public static final String OTHER_USER_EMAIL = "jane@example.com";
    public static final int OTHER_USER_AGE = 25;

    public static final long VALID_ID = 1L;
    public static final long NON_EXISTENT_ID = -1L;
    public static final String INVALID_EMAIL = "invalid-email";
    public static final int MIN_VALID_AGE = 0;
    public static final int INVALID_AGE = -1;
    public static final String DB_ERROR_MESSAGE = "Database unavailable";

    private UserTestFactory() {
    }

    public static User createUser() {
        return createUser(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
    }

    public static User createUser(String name, String email, int age) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setAge(age);
        return user;
    }
}
