package org.example.userservice.hateoas;

import lombok.RequiredArgsConstructor;
import org.example.userservice.controller.UserController;
import org.example.userservice.database.entity.User;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.mapper.UserMapper;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

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

    @Override
    public CollectionModel<EntityModel<UserResponse>> toCollectionModel(Iterable<? extends User> users) {
        return RepresentationModelAssembler.super.toCollectionModel(users)
                .add(linkTo(methodOn(UserController.class).getAll()).withSelfRel());
    }
}
