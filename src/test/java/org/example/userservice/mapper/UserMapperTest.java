package org.example.userservice.mapper;

import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.junit.jupiter.api.Test;

import static org.example.userservice.fixture.UserTestFactory.createUser;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.VALID_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toEntity_mapsRequestFields() {
        CreateUserRq request = new CreateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);

        User user = userMapper.toEntity(request);

        assertEquals(request.name(), user.getName());
        assertEquals(request.email(), user.getEmail());
        assertEquals(request.age(), user.getAge());
    }

    @Test
    void updateEntity_updatesUserFields() {
        User user = createUser();
        UpdateUserRq request = new UpdateUserRq(VALID_ID, OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE);

        userMapper.updateEntity(user, request);

        assertEquals(request.name(), user.getName());
        assertEquals(request.email(), user.getEmail());
        assertEquals(request.age(), user.getAge());
    }
}
