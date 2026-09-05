package org.example.userservice.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import org.example.userservice.database.dao.UserDao;
import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.exception.UserServiceException;
import org.example.userservice.mapper.UserMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.example.userservice.constant.ErrorCode.USER_NOT_FOUND;
import static org.example.userservice.constant.ErrorCode.VALIDATION_ERROR;

@AllArgsConstructor
public class UserService {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final UserDao userDao;
    private final UserMapper userMapper;

    public User createUser(CreateUserRq request) {
        validate(request);
        User user = userMapper.toEntity(request);
        return userDao.create(user);
    }

    public Optional<User> getUserById(Long id) {
        validateId(id);
        return userDao.findById(id);
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    public User updateUser(UpdateUserRq request) {
        validate(request);

        User user = userDao.findById(request.id())
                .orElseThrow(() -> new UserServiceException(USER_NOT_FOUND, USER_NOT_FOUND.getMessage()));

        userMapper.updateEntity(user, request);

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

    private <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.iterator().next().getMessage();
            throw new UserServiceException(VALIDATION_ERROR, message);
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
    }
}