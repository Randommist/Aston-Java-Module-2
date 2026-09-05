package org.example.userservice.mapper;

import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;

import java.time.LocalDateTime;

public class UserMapper {
    /**
     * Создаёт новую сущность User из данных запроса
     * @param request
     */
    public User toEntity(CreateUserRq request) {
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim());
        user.setAge(request.age());
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Обновление полей существующей сущности User
     * @param user
     * @param request
     */
    public void updateEntity(User user, UpdateUserRq request) {
        user.setName(request.name().trim());
        user.setEmail(request.email().trim());
        user.setAge(request.age());
    }
}