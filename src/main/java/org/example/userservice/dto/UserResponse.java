package org.example.userservice.dto;

import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

@Relation(itemRelation = "user", collectionRelation = "users")
public record UserResponse(
        Long id,
        String name,
        String email,
        Integer age,
        LocalDateTime createdAt
) {
}
