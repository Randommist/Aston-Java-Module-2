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

import static org.example.userservice.constant.ErrorCode.DB_ERROR;
import static org.example.userservice.constant.ErrorCode.USER_NOT_FOUND;
import static org.example.userservice.constant.ErrorCode.VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String VALID_NAME = "Andrey";
    private static final String VALID_EMAIL = "andrey@example.com";
    private static final String INVALID_EMAIL = "invalid-email";
    private static final int VALID_AGE = 30;
    private static final int MIN_VALID_AGE = 0;
    private static final int INVALID_AGE = -1;
    private static final long VALID_ID = 1L;
    private static final String DB_ERROR_MESSAGE = "Database unavailable";

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
    @ValueSource(ints = {MIN_VALID_AGE, VALID_AGE})
    void createUser_validRequest_returnsDaoResult(int age) {
        CreateUserRq request = new CreateUserRq(VALID_NAME, VALID_EMAIL, age);
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
                new CreateUserRq(null, VALID_EMAIL, VALID_AGE),
                new CreateUserRq("", VALID_EMAIL, VALID_AGE),
                new CreateUserRq("   ", VALID_EMAIL, VALID_AGE),
                new CreateUserRq(VALID_NAME, null, VALID_AGE),
                new CreateUserRq(VALID_NAME, "", VALID_AGE),
                new CreateUserRq(VALID_NAME, "   ", VALID_AGE),
                new CreateUserRq(VALID_NAME, INVALID_EMAIL, VALID_AGE),
                new CreateUserRq(VALID_NAME, VALID_EMAIL, INVALID_AGE));
    }

    @Test
    void createUser_daoFailure_propagatesException() {
        CreateUserRq request = new CreateUserRq(VALID_NAME, VALID_EMAIL, VALID_AGE);
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
        when(userDao.findById(VALID_ID)).thenReturn(Optional.of(user));

        assertSame(user, userService.getUserById(VALID_ID).orElseThrow());

        verify(userDao).findById(VALID_ID);
        verifyNoInteractions(userMapper);
    }

    @Test
    void getUserById_missingUser_returnsEmpty() {
        when(userDao.findById(VALID_ID)).thenReturn(Optional.empty());

        assertTrue(userService.getUserById(VALID_ID).isEmpty());
        verify(userDao).findById(VALID_ID);
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
        when(userDao.findById(VALID_ID)).thenThrow(failure);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.getUserById(VALID_ID)));
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
    @ValueSource(ints = {MIN_VALID_AGE, VALID_AGE})
    void updateUser_existingUser_mapsBeforeSavingAndReturnsDaoResult(int age) {
        UpdateUserRq request = new UpdateUserRq(VALID_ID, VALID_NAME, VALID_EMAIL, age);
        User existing = new User();
        User saved = new User();
        when(userDao.findById(VALID_ID)).thenReturn(Optional.of(existing));
        when(userDao.update(existing)).thenReturn(saved);

        assertSame(saved, userService.updateUser(request));

        var order = inOrder(userDao, userMapper);
        order.verify(userDao).findById(VALID_ID);
        order.verify(userMapper).updateEntity(existing, request);
        order.verify(userDao).update(existing);
    }

    @Test
    void updateUser_missingUser_rejectsWithoutMappingOrSaving() {
        UpdateUserRq request = new UpdateUserRq(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_AGE);
        when(userDao.findById(VALID_ID)).thenReturn(Optional.empty());

        UserServiceException error = assertThrows(UserServiceException.class,
                () -> userService.updateUser(request));

        assertEquals(USER_NOT_FOUND, error.getErrorCode());
        verify(userDao).findById(VALID_ID);
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
                new UpdateUserRq(null, VALID_NAME, VALID_EMAIL, VALID_AGE),
                new UpdateUserRq(0L, VALID_NAME, VALID_EMAIL, VALID_AGE),
                new UpdateUserRq(-1L, VALID_NAME, VALID_EMAIL, VALID_AGE),
                new UpdateUserRq(VALID_ID, null, VALID_EMAIL, VALID_AGE),
                new UpdateUserRq(VALID_ID, "", VALID_EMAIL, VALID_AGE),
                new UpdateUserRq(VALID_ID, "   ", VALID_EMAIL, VALID_AGE),
                new UpdateUserRq(VALID_ID, VALID_NAME, null, VALID_AGE),
                new UpdateUserRq(VALID_ID, VALID_NAME, "", VALID_AGE),
                new UpdateUserRq(VALID_ID, VALID_NAME, "   ", VALID_AGE),
                new UpdateUserRq(VALID_ID, VALID_NAME, INVALID_EMAIL, VALID_AGE),
                new UpdateUserRq(VALID_ID, VALID_NAME, VALID_EMAIL, INVALID_AGE));
    }

    @Test
    void updateUser_lookupFailure_propagatesWithoutMappingOrSaving() {
        UpdateUserRq request = new UpdateUserRq(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_AGE);
        UserServiceException failure = databaseFailure();
        when(userDao.findById(VALID_ID)).thenThrow(failure);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.updateUser(request)));
        verify(userDao).findById(VALID_ID);
        verifyNoMoreInteractions(userDao);
        verifyNoInteractions(userMapper);
    }

    @Test
    void updateUser_saveFailure_propagatesException() {
        UpdateUserRq request = new UpdateUserRq(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_AGE);
        User existing = new User();
        UserServiceException failure = databaseFailure();
        when(userDao.findById(VALID_ID)).thenReturn(Optional.of(existing));
        when(userDao.update(existing)).thenThrow(failure);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.updateUser(request)));
        verify(userMapper).updateEntity(existing, request);
    }

    @Test
    void deleteUser_successfulDaoCall_returnsTrueWithoutExistenceCheck() {
        assertTrue(userService.deleteUser(VALID_ID));

        verify(userDao).deleteById(VALID_ID);
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
        doThrow(failure).when(userDao).deleteById(VALID_ID);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.deleteUser(VALID_ID)));
    }

    private static UserServiceException databaseFailure() {
        return new UserServiceException(DB_ERROR, DB_ERROR_MESSAGE);
    }
}