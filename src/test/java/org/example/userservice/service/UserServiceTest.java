package org.example.userservice.service;

import org.example.userservice.database.dao.UserDao;
import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.exception.UserServiceException;
import org.example.userservice.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.example.userservice.constant.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDao, userMapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 30})
    void createUser_validRequest_returnsDaoResult(int age) {
        CreateUserRq request = new CreateUserRq("Andrey", "andrey@example.com", age);
        User mapped = new User();
        User saved = new User();
        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(userDao.create(mapped)).thenReturn(saved);

        assertSame(saved, userService.createUser(request));

        verify(userMapper).toEntity(request);
        verify(userDao).create(mapped);
    }

    @ParameterizedTest
    @MethodSource("invalidCreateRequests")
    void createUser_invalidRequest_rejectsBeforeMappingOrPersistence(CreateUserRq request) {
        UserServiceException error = assertThrows(UserServiceException.class,
                () -> userService.createUser(request));

        assertEquals(VALIDATION_ERROR, error.getErrorCode());
        verifyNoInteractions(userDao, userMapper);
    }

    static Stream<CreateUserRq> invalidCreateRequests() {
        return Stream.of(
                new CreateUserRq(null, "andrey@example.com", 30),
                new CreateUserRq("", "andrey@example.com", 30),
                new CreateUserRq("   ", "andrey@example.com", 30),
                new CreateUserRq("Andrey", null, 30),
                new CreateUserRq("Andrey", "", 30),
                new CreateUserRq("Andrey", "   ", 30),
                new CreateUserRq("Andrey", "invalid-email", 30),
                new CreateUserRq("Andrey", "andrey@example.com", -1));
    }

    @Test
    void createUser_daoFailure_propagatesException() {
        CreateUserRq request = new CreateUserRq("Andrey", "andrey@example.com", 30);
        User mapped = new User();
        UserServiceException failure = databaseFailure();
        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(userDao.create(mapped)).thenThrow(failure);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.createUser(request)));
    }

    @Test
    void getUserById_existingUser_returnsUser() {
        User user = new User();
        when(userDao.findById(1L)).thenReturn(Optional.of(user));

        assertSame(user, userService.getUserById(1L).orElseThrow());

        verify(userDao).findById(1L);
        verifyNoInteractions(userMapper);
    }

    @Test
    void getUserById_missingUser_returnsEmpty() {
        when(userDao.findById(1L)).thenReturn(Optional.empty());

        assertTrue(userService.getUserById(1L).isEmpty());
        verify(userDao).findById(1L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void getUserById_invalidId_rejectsBeforeDaoCall(Long id) {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(id));

        verifyNoInteractions(userDao, userMapper);
    }

    @Test
    void getUserById_daoFailure_propagatesException() {
        UserServiceException failure = databaseFailure();
        when(userDao.findById(1L)).thenThrow(failure);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.getUserById(1L)));
    }

    @Test
    void getAllUsers_returnsDaoUsers() {
        List<User> users = List.of(new User(), new User());
        when(userDao.findAll()).thenReturn(users);

        assertEquals(users, userService.getAllUsers());
        verify(userDao).findAll();
        verifyNoInteractions(userMapper);
    }

    @Test
    void getAllUsers_emptyDatabase_returnsEmptyList() {
        when(userDao.findAll()).thenReturn(List.of());

        assertTrue(userService.getAllUsers().isEmpty());
        verify(userDao).findAll();
    }

    @Test
    void getAllUsers_daoFailure_propagatesException() {
        UserServiceException failure = databaseFailure();
        when(userDao.findAll()).thenThrow(failure);

        assertSame(failure, assertThrows(UserServiceException.class, userService::getAllUsers));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 30})
    void updateUser_existingUser_mapsBeforeSavingAndReturnsDaoResult(int age) {
        UpdateUserRq request = new UpdateUserRq(1L, "Andrey", "andrey@example.com", age);
        User existing = new User();
        User saved = new User();
        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.update(existing)).thenReturn(saved);

        assertSame(saved, userService.updateUser(request));

        var order = inOrder(userDao, userMapper);
        order.verify(userDao).findById(1L);
        order.verify(userMapper).updateEntity(existing, request);
        order.verify(userDao).update(existing);
    }

    @Test
    void updateUser_missingUser_rejectsWithoutMappingOrSaving() {
        UpdateUserRq request = new UpdateUserRq(1L, "Andrey", "andrey@example.com", 30);
        when(userDao.findById(1L)).thenReturn(Optional.empty());

        UserServiceException error = assertThrows(UserServiceException.class,
                () -> userService.updateUser(request));

        assertEquals(USER_NOT_FOUND, error.getErrorCode());
        verify(userDao).findById(1L);
        verifyNoMoreInteractions(userDao);
        verifyNoInteractions(userMapper);
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateRequests")
    void updateUser_invalidRequest_rejectsBeforeLookupOrMapping(UpdateUserRq request) {
        UserServiceException error = assertThrows(UserServiceException.class,
                () -> userService.updateUser(request));

        assertEquals(VALIDATION_ERROR, error.getErrorCode());
        verifyNoInteractions(userDao, userMapper);
    }

    static Stream<UpdateUserRq> invalidUpdateRequests() {
        return Stream.of(
                new UpdateUserRq(null, "Andrey", "andrey@example.com", 30),
                new UpdateUserRq(0L, "Andrey", "andrey@example.com", 30),
                new UpdateUserRq(-1L, "Andrey", "andrey@example.com", 30),
                new UpdateUserRq(1L, null, "andrey@example.com", 30),
                new UpdateUserRq(1L, "", "andrey@example.com", 30),
                new UpdateUserRq(1L, "   ", "andrey@example.com", 30),
                new UpdateUserRq(1L, "Andrey", null, 30),
                new UpdateUserRq(1L, "Andrey", "", 30),
                new UpdateUserRq(1L, "Andrey", "   ", 30),
                new UpdateUserRq(1L, "Andrey", "invalid-email", 30),
                new UpdateUserRq(1L, "Andrey", "andrey@example.com", -1));
    }

    @Test
    void updateUser_lookupFailure_propagatesWithoutMappingOrSaving() {
        UpdateUserRq request = new UpdateUserRq(1L, "Andrey", "andrey@example.com", 30);
        UserServiceException failure = databaseFailure();
        when(userDao.findById(1L)).thenThrow(failure);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.updateUser(request)));
        verify(userDao).findById(1L);
        verifyNoMoreInteractions(userDao);
        verifyNoInteractions(userMapper);
    }

    @Test
    void updateUser_saveFailure_propagatesException() {
        UpdateUserRq request = new UpdateUserRq(1L, "Andrey", "andrey@example.com", 30);
        User existing = new User();
        UserServiceException failure = databaseFailure();
        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.update(existing)).thenThrow(failure);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.updateUser(request)));
        verify(userMapper).updateEntity(existing, request);
    }

    @Test
    void deleteUser_successfulDaoCall_returnsTrueWithoutExistenceCheck() {
        assertTrue(userService.deleteUser(1L));

        verify(userDao).deleteById(1L);
        verifyNoMoreInteractions(userDao);
        verifyNoInteractions(userMapper);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void deleteUser_invalidId_rejectsBeforeDaoCall(Long id) {
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(id));

        verifyNoInteractions(userDao, userMapper);
    }

    @Test
    void deleteUser_daoFailure_propagatesException() {
        UserServiceException failure = databaseFailure();
        doThrow(failure).when(userDao).deleteById(1L);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.deleteUser(1L)));
    }

    private static UserServiceException databaseFailure() {
        return new UserServiceException(DB_ERROR, "Database unavailable");
    }
}
