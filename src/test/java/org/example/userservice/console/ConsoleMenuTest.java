package org.example.userservice.console;

import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.VALID_ID;
import static org.example.userservice.fixture.UserTestFactory.createUser;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsoleMenuTest {

    @Mock
    private UserService userService;

    private final User user = createUser();

    @Test
    void run_exit_doesNotCallService() {
        runMenu("0\n");

        verifyNoInteractions(userService);
    }

    @Test
    void run_createUser_passesEnteredFields() {
        CreateUserRq request = new CreateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
        when(userService.createUser(request)).thenReturn(user);

        runMenu("1\n%s\n%s\n%d\n0\n".formatted(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE));

        verify(userService).createUser(request);
    }

    @Test
    void run_findUser_passesEnteredId() {
        when(userService.getUserById(VALID_ID)).thenReturn(Optional.of(user));

        runMenu("2\n%d\n0\n".formatted(VALID_ID));

        verify(userService).getUserById(VALID_ID);
    }

    @Test
    void run_showAllUsers_callsService() {
        when(userService.getAllUsers()).thenReturn(List.of(user));

        runMenu("3\n0\n");

        verify(userService).getAllUsers();
    }

    @Test
    void run_updateUser_passesEnteredFields() {
        UpdateUserRq request = new UpdateUserRq(OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE);
        when(userService.updateUser(VALID_ID, request)).thenReturn(user);

        runMenu("4\n%d\n%s\n%s\n%d\n0\n".formatted(VALID_ID, OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE));

        verify(userService).updateUser(VALID_ID, request);
    }

    @Test
    void run_deleteUser_passesEnteredId() {
        runMenu("5\n%d\n0\n".formatted(VALID_ID));

        verify(userService).deleteUser(VALID_ID);
    }

    @Test
    void run_invalidId_doesNotCallServiceAndAllowsExit() {
        runMenu("2\ninvalid\n0\n");

        verifyNoInteractions(userService);
    }

    private void runMenu(String input) {
        try (Scanner scanner = new Scanner(input)) {
            new ConsoleMenu(userService, scanner).run();
        }
    }
}
