package org.example.userservice.service;

import org.example.userservice.dao.UserDao;
import org.example.userservice.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User createUser(String name, String email, int age) {
        validateUserData(name, email, age);

        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.trim());
        user.setAge(age);
        user.setCreatedAt(LocalDateTime.now());

        return userDao.create(user);
    }

    public Optional<User> getUserById(Long id) {
        validateId(id);
        return userDao.findById(id);
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    public User updateUser(Long id, String name, String email, int age) {
        validateId(id);
        validateUserData(name, email, age);

        User user = userDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + id + " not found"));

        user.setName(name.trim());
        user.setEmail(email.trim());
        user.setAge(age);

        return userDao.update(user);
    }

    public boolean deleteUser(Long id) {
        validateId(id);

        if (userDao.findById(id).isEmpty()) {
            return false;
        }

        userDao.deleteById(id);
        return true;
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
    }

    private void validateUserData(String name, String email, int age) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Email must be valid");
        }

        if (age < 0) {
            throw new IllegalArgumentException("Age must not be negative");
        }
    }
}
