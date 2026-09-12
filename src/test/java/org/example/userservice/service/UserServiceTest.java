package org.example.userservice.service;

import org.example.userservice.database.dao.UserDao;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.database.entity.User;
import org.example.userservice.exception.UserServiceException;
import org.example.userservice.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private static final String DEFAULT_USER_NAME = "Joe";
    private static final String DEFAULT_USER_EMAIL = "Peach@example.com";
    private static final int DEFAULT_USER_AGE = 18;

    private static final String OTHER_USER_NAME = "Carl";
    private static final String OTHER_USER_EMAIL = "carl@example.com";
    private static final int OTHER_USER_AGE = 30;

    private static final long NON_EXISTENT_ID = -1L;

    @Test
    @DisplayName("createUser: корректный запрос должен создать пользователя")
    void createUser_validRequest_returnsCreatedUser() {
        // given
        CreateUserRq request = new CreateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
        User userEntity = new User();
        User createdUser = new User();
        when(userMapper.toEntity(request)).thenReturn(userEntity);
        when(userDao.create(userEntity)).thenReturn(createdUser);

        // when
        User result = userService.createUser(request);

        // then
        assertSame(createdUser, result);
    }

    @Test
    @DisplayName("createUser: некорректный запрос должен бросать UserServiceException")
    void createUser_invalidRequest_throwsUserServiceException() {
        // given
        CreateUserRq request = new CreateUserRq(null, null, 0);

        // when / then
        assertThrows(UserServiceException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("getUserById: поиск по корректному id должен найти сохраненного пользователя")
    void getUserById_validId_returnsUser() {
        // given
        Long id = 1L;
        User user = new User();
        when(userDao.findById(id)).thenReturn(Optional.of(user));

        // when
        Optional<User> result = userService.getUserById(id);

        // then
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("getUserById: поиск по некорректному id должен бросать IllegalArgumentException")
    void getUserById_invalidId_throwsIllegalArgumentException() {
        // given
        Long invalidId = 0L;

        // when / then
        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(invalidId));
    }

    @Test
    @DisplayName("getAllUsers: вывод всех пользователей должен вернуть список сохраненных пользователей")
    void getAllUsers_noArgs_returnsListOfUsers() {
        // given
        List<User> users = List.of(new User(), new User());
        when(userDao.findAll()).thenReturn(users);

        // when
        List<User> result = userService.getAllUsers();

        // then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("updateUser: корректный запрос должен обновить данные у существующего пользователя")
    void updateUser_validRequest_returnsUpdatedUser() {
        // given
        UpdateUserRq request = new UpdateUserRq(1L, OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE);
        User existingUser = new User();
        User updatedUser = new User();

        when(userDao.findById(request.id())).thenReturn(Optional.of(existingUser));
        when(userDao.update(existingUser)).thenReturn(updatedUser);

        // when
        User result = userService.updateUser(request);

        // then
        assertSame(updatedUser, result);
    }

    @Test
    @DisplayName("updateUser: обновление несуществующего пользователя должно бросать UserServiceException")
    void updateUser_userNotFound_throwsUserServiceException() {
        // given
        UpdateUserRq request = new UpdateUserRq(999L, OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE);
        when(userDao.findById(request.id())).thenReturn(Optional.empty());

        // when / then
        assertThrows(UserServiceException.class, () -> userService.updateUser(request));
    }

    @Test
    @DisplayName("deleteUser: после удаления пользователь не должен находиться по id")
    void deleteUser_validId_returnsTrue() {
        // given
        Long id = 1L;

        // when
        userService.deleteUser(id);

        // then
        assertTrue(userDao.findById(id).isEmpty());
    }

    @Test
    @DisplayName("deleteUser: удаление пользователя по несуществующему id должно бросать IllegalArgumentException")
    void deleteUser_invalidId_throwsIllegalArgumentException() {
        // given
        Long invalidId = NON_EXISTENT_ID;

        // when / then
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(invalidId));
    }
}