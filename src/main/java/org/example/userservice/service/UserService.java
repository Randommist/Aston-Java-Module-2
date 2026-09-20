package org.example.userservice.service;

import lombok.AllArgsConstructor;
import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.exception.UserServiceException;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.example.userservice.constant.ErrorCode.USER_NOT_FOUND;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public User createUser(CreateUserRq request) {
        User user = userMapper.toEntity(request);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        validateId(id);
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRq request) {
        validateId(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserServiceException(USER_NOT_FOUND, USER_NOT_FOUND.getMessage()));

        userMapper.updateEntity(user, request);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        validateId(id);
        if (!userRepository.existsById(id)) {
            throw new UserServiceException(USER_NOT_FOUND, USER_NOT_FOUND.getMessage());
        }
        userRepository.deleteById(id);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
    }
}
