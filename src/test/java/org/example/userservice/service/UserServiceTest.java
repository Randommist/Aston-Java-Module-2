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

import static org.example.userservice.fixture.UserTestFactory.createUser;
import static org.example.userservice.fixture.UserTestFactory.DB_ERROR_MESSAGE;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.INVALID_AGE;
import static org.example.userservice.fixture.UserTestFactory.INVALID_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.MIN_VALID_AGE;
import static org.example.userservice.fixture.UserTestFactory.VALID_ID;
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
    @ValueSource(ints = {MIN_VALID_AGE, DEFAULT_USER_AGE})
    void createUser_validRequest_returnsDaoResult(int age) {
        CreateUserRq request = new CreateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, age);
        User mapped = createUser();
        User saved = createUser();
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
                new CreateUserRq(null, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                new CreateUserRq("", DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                new CreateUserRq("   ", DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                new CreateUserRq(DEFAULT_USER_NAME, null, DEFAULT_USER_AGE),
                new CreateUserRq(DEFAULT_USER_NAME, "", DEFAULT_USER_AGE),
                new CreateUserRq(DEFAULT_USER_NAME, "   ", DEFAULT_USER_AGE),
                new CreateUserRq(DEFAULT_USER_NAME, INVALID_EMAIL, DEFAULT_USER_AGE),
                new CreateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, INVALID_AGE));
    }

    @Test
    void createUser_daoFailure_propagatesException() {
        CreateUserRq request = new CreateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
        User mapped = createUser();
        UserServiceException failure = databaseFailure();
        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(userDao.create(mapped)).thenThrow(failure);

        assertSame(failure, assertThrows(UserServiceException.class,
                () -> userService.createUser(request)));
    }

    @Test
    void getUserById_existingUser_returnsUser() {
        User user = createUser();
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
        List<User> users = List.of(createUser(), createUser());
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
    @ValueSource(ints = {MIN_VALID_AGE, DEFAULT_USER_AGE})
    void updateUser_existingUser_mapsBeforeSavingAndReturnsDaoResult(int age) {
        UpdateUserRq request = new UpdateUserRq(VALID_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, age);
        User existing = createUser();
        User saved = createUser();
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
        UpdateUserRq request = new UpdateUserRq(VALID_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
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
                new UpdateUserRq(null, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                new UpdateUserRq(0L, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                new UpdateUserRq(-1L, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                new UpdateUserRq(VALID_ID, null, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                new UpdateUserRq(VALID_ID, "", DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                new UpdateUserRq(VALID_ID, "   ", DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                new UpdateUserRq(VALID_ID, DEFAULT_USER_NAME, null, DEFAULT_USER_AGE),
                new UpdateUserRq(VALID_ID, DEFAULT_USER_NAME, "", DEFAULT_USER_AGE),
                new UpdateUserRq(VALID_ID, DEFAULT_USER_NAME, "   ", DEFAULT_USER_AGE),
                new UpdateUserRq(VALID_ID, DEFAULT_USER_NAME, INVALID_EMAIL, DEFAULT_USER_AGE),
                new UpdateUserRq(VALID_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, INVALID_AGE));
    }

    @Test
    void updateUser_lookupFailure_propagatesWithoutMappingOrSaving() {
        UpdateUserRq request = new UpdateUserRq(VALID_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
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
        UpdateUserRq request = new UpdateUserRq(VALID_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
        User existing = createUser();
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
