package org.example.userservice.service;

import org.example.userservice.dao.UserDao;
import org.example.userservice.entity.User;
import org.example.userservice.exception.UserServiceException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    @Test
    void shouldCreateUserThroughDao() {
        InMemoryUserDao userDao = new InMemoryUserDao();
        UserService userService = new UserService(userDao);

        User user = userService.createUser("Test User", "test@example.com", 25);

        assertNotNull(user.getId());
        assertNotNull(user.getCreatedAt());
        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(25, user.getAge());
    }

    @Test
    void shouldDeleteExistingUser() {
        InMemoryUserDao userDao = new InMemoryUserDao();
        UserService userService = new UserService(userDao);
        User user = userService.createUser("Test User", "test@example.com", 25);

        assertTrue(userService.deleteUser(user.getId()));
        assertFalse(userService.getUserById(user.getId()).isPresent());
    }

    @Test
    void shouldThrowServiceExceptionWhenUpdatingMissingUser() {
        UserService userService = new UserService(new InMemoryUserDao());

        UserServiceException exception = assertThrows(
                UserServiceException.class,
                () -> userService.updateUser(1L, "Test User", "test@example.com", 25)
        );

        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void shouldRejectInvalidEmail() {
        UserService userService = new UserService(new InMemoryUserDao());

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("Test User", "invalid@", 25)
        );
    }

    @Test
    void shouldAcceptValidEmail() {
        UserService userService = new UserService(new InMemoryUserDao());

        User user = userService.createUser("Test User", "test.user+tag@example.com", 25);

        assertEquals("test.user+tag@example.com", user.getEmail());
    }

    private static class InMemoryUserDao implements UserDao {
        private final List<User> users = new ArrayList<>();
        private long nextId = 1;

        @Override
        public User create(User user) {
            user.setId(nextId++);
            users.add(user);
            return user;
        }

        @Override
        public Optional<User> findById(Long id) {
            return users.stream()
                    .filter(user -> user.getId().equals(id))
                    .findFirst();
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(users);
        }

        @Override
        public User update(User user) {
            return user;
        }

        @Override
        public void deleteById(Long id) {
            users.removeIf(user -> user.getId().equals(id));
        }
    }
}
