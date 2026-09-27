package org.example.userservice.service;

import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.event.UserDeletedEvent;
import org.example.userservice.event.UserCreatedEvent;
import org.example.userservice.exception.UserServiceException;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.example.userservice.constant.ErrorCode.USER_NOT_FOUND;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.VALID_ID;
import static org.example.userservice.fixture.UserTestFactory.createUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userMapper, eventPublisher);
    }

    @Test
    void createUser_mapsAndSavesEntity() {
        CreateUserRq request = new CreateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
        User entity = createUser();
        User saved = createUser();
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.save(entity)).thenReturn(saved);

        assertSame(saved, userService.createUser(request));

        InOrder order = inOrder(userMapper, userRepository);
        order.verify(userMapper).toEntity(request);
        order.verify(userRepository).save(entity);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertInstanceOf(UserCreatedEvent.class, captor.getValue());
        assertEquals(DEFAULT_USER_EMAIL, ((UserCreatedEvent) captor.getValue()).email());
    }

    @Test
    void getUserById_returnsRepositoryResult() {
        User user = createUser();
        when(userRepository.findById(VALID_ID)).thenReturn(Optional.of(user));

        assertSame(user, userService.getUserById(VALID_ID).orElseThrow());
    }

    @Test
    void getAllUsers_returnsRepositoryUsers() {
        List<User> users = List.of(createUser(), createUser());
        when(userRepository.findAll()).thenReturn(users);

        assertEquals(users, userService.getAllUsers());
    }

    @Test
    void updateUser_updatesManagedEntityWithoutSavingAgain() {
        UpdateUserRq request = new UpdateUserRq(OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE);
        User existing = createUser();
        when(userRepository.findById(VALID_ID)).thenReturn(Optional.of(existing));

        assertSame(existing, userService.updateUser(VALID_ID, request));

        InOrder order = inOrder(userRepository, userMapper);
        order.verify(userRepository).findById(VALID_ID);
        order.verify(userMapper).updateEntity(existing, request);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_missingUser_throwsNotFound() {
        UpdateUserRq request = new UpdateUserRq(OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE);
        when(userRepository.findById(VALID_ID)).thenReturn(Optional.empty());

        UserServiceException exception = assertThrows(UserServiceException.class,
                () -> userService.updateUser(VALID_ID, request));

        assertEquals(USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_existingUser_deletesIt() {
        User user = new User();
        user.setId(VALID_ID);
        user.setEmail("test@example.com");

        when(userRepository.findById(VALID_ID)).thenReturn(Optional.of(user));

        userService.deleteUser(VALID_ID);

        verify(userRepository).deleteById(VALID_ID);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());

        Object published = captor.getValue();
        assertInstanceOf(UserDeletedEvent.class, published);
        UserDeletedEvent event = (UserDeletedEvent) published;
        assertEquals("test@example.com", event.email());
    }

    @Test
    void deleteUser_missingUser_throwsNotFound() {
        when(userRepository.findById(VALID_ID)).thenReturn(Optional.empty());

        UserServiceException exception = assertThrows(UserServiceException.class,
                () -> userService.deleteUser(VALID_ID));

        assertEquals(USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).deleteById(VALID_ID);
    }

    @Test
    void invalidId_isRejectedBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(0L));
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(-1L,
                new UpdateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE)));
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(null));

        assertTrue(org.mockito.Mockito.mockingDetails(userRepository).getInvocations().isEmpty());
    }
}
