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
import org.example.userservice.hateoas.UserModelAssembler;
import org.example.userservice.service.UserService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import static org.example.userservice.constant.ErrorCode.USER_NOT_FOUND;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Управление пользователями")
public class UserController {

    private final UserService userService;
    private final UserModelAssembler userModelAssembler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать пользователя")
    public EntityModel<UserResponse> create(@Valid @RequestBody CreateUserRq request) {
        return userModelAssembler.toModel(userService.createUser(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по id")
    public EntityModel<UserResponse> getById(@PathVariable @Positive Long id) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new UserServiceException(USER_NOT_FOUND, USER_NOT_FOUND.getMessage()));
        return userModelAssembler.toModel(user);
    }

    @GetMapping
    @Operation(summary = "Получить всех пользователей")
    public CollectionModel<EntityModel<UserResponse>> getAll() {
        return userModelAssembler.toCollectionModel(userService.getAllUsers());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя")
    public EntityModel<UserResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateUserRq request
    ) {
        return userModelAssembler.toModel(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
