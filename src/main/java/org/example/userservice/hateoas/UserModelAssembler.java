package org.example.userservice.hateoas;

import lombok.RequiredArgsConstructor;
import org.example.userservice.controller.UserController;
import org.example.userservice.database.entity.User;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.mapper.UserMapper;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Превращает {@link User} в ответ API со ссылками на доступные действия.
 */
@Component
@RequiredArgsConstructor
public class UserModelAssembler implements RepresentationModelAssembler<User, EntityModel<UserResponse>> {

    private final UserMapper userMapper;

    @Override
    public EntityModel<UserResponse> toModel(User user) {
        Long id = user.getId();
        return EntityModel.of(userMapper.toResponse(user),
                linkTo(methodOn(UserController.class).getById(id)).withSelfRel(),
                linkTo(methodOn(UserController.class).update(id, null)).withRel("update"),
                linkTo(methodOn(UserController.class).delete(id)).withRel("delete"),
                linkTo(methodOn(UserController.class).getAll()).withRel("users"));
    }

    /**
     * Список пользователей со ссылкой на себя. Для пустого списка явно отдаём
     * пустой {@code _embedded.users}: иначе HAL опустил бы это поле целиком.
     */
    public CollectionModel<?> toUsersModel(List<User> users) {
        Link selfLink = linkTo(methodOn(UserController.class).getAll()).withSelfRel();
        if (users.isEmpty()) {
            Object emptyUsers = new EmbeddedWrappers(false).emptyCollectionOf(UserResponse.class);
            return CollectionModel.of(List.of(emptyUsers), selfLink);
        }
        return toCollectionModel(users).add(selfLink);
    }
}
