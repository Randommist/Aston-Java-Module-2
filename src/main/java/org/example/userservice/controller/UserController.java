package org.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.exception.UserServiceException;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.example.userservice.constant.ErrorCode.USER_NOT_FOUND;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Управление пользователями")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать пользователя")
    public UserResponse create(@Valid @RequestBody CreateUserRq request) {
        return userMapper.toResponse(userService.createUser(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по id")
    public UserResponse getById(@PathVariable @Positive Long id) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new UserServiceException(USER_NOT_FOUND, USER_NOT_FOUND.getMessage()));
        return userMapper.toResponse(user);
    }

    @GetMapping
    @Operation(summary = "Получить всех пользователей")
    public List<UserResponse> getAll() {
        return userService.getAllUsers().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя")
    public UserResponse update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateUserRq request
    ) {
        return userMapper.toResponse(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить пользователя")
    public void delete(@PathVariable @Positive Long id) {
        userService.deleteUser(id);
    }
}
