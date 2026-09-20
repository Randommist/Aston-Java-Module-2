package org.example.userservice.mapper;

import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
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

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getCreatedAt()
        );
    }
}
