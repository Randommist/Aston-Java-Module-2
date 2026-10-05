package org.example.userservice.controller;

import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.hateoas.UserModelAssembler;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.VALID_ID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({UserModelAssembler.class, UserMapper.class})
class UserControllerTest {

    private static final String USERS_URL = "http://localhost/api/users";
    private static final String USER_URL = USERS_URL + "/" + VALID_ID;

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void create_returnsCreatedDto() throws Exception {
        CreateUserRq request = new CreateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
        when(userService.createUser(request)).thenReturn(user(VALID_ID, DEFAULT_USER_NAME,
                DEFAULT_USER_EMAIL, DEFAULT_USER_AGE));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"John","email":"john@example.com","age":30}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.id").value(VALID_ID))
                .andExpect(jsonPath("$.name").value(DEFAULT_USER_NAME))
                .andExpect(jsonPath("$.email").value(DEFAULT_USER_EMAIL))
                .andExpect(jsonPath("$._links.self.href").value(USER_URL));
    }

    @Test
    void create_invalidBody_returnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"wrong","age":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.age").exists());
    }

    @Test
    void getById_returnsDto() throws Exception {
        when(userService.getUserById(VALID_ID)).thenReturn(Optional.of(user(VALID_ID,
                DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE)));

        mockMvc.perform(get("/api/users/{id}", VALID_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_ID))
                .andExpect(jsonPath("$.age").value(DEFAULT_USER_AGE))
                .andExpect(jsonPath("$._links.self.href").value(USER_URL))
                .andExpect(jsonPath("$._links.update.href").value(USER_URL))
                .andExpect(jsonPath("$._links.delete.href").value(USER_URL))
                .andExpect(jsonPath("$._links.users.href").value(USERS_URL));
    }

    @Test
    void getById_missingUser_returnsNotFound() throws Exception {
        when(userService.getUserById(VALID_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{id}", VALID_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void getAll_returnsDtoList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                user(VALID_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE),
                user(2L, OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users.length()").value(2))
                .andExpect(jsonPath("$._embedded.users[1].name").value(OTHER_USER_NAME))
                .andExpect(jsonPath("$._embedded.users[1]._links.self.href").value(USERS_URL + "/2"))
                .andExpect(jsonPath("$._links.self.href").value(USERS_URL));
    }

    @Test
    void update_returnsUpdatedDto() throws Exception {
        UpdateUserRq request = new UpdateUserRq(OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE);
        when(userService.updateUser(VALID_ID, request)).thenReturn(user(VALID_ID,
                OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE));

        mockMvc.perform(put("/api/users/{id}", VALID_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jane","email":"jane@example.com","age":25}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_ID))
                .andExpect(jsonPath("$.name").value(OTHER_USER_NAME))
                .andExpect(jsonPath("$._links.self.href").value(USER_URL));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", VALID_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).deleteUser(VALID_ID);
    }

    @Test
    void duplicateEmail_returnsConflict() throws Exception {
        CreateUserRq request = new CreateUserRq(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(userService).createUser(request);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"John","email":"john@example.com","age":30}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }

    private User user(Long id, String name, String email, int age) {
        return new User(id, name, email, age, LocalDateTime.of(2026, 1, 1, 12, 0));
    }
}
