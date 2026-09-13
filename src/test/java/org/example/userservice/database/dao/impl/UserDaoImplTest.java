package org.example.userservice.database.dao.impl;

import config.TestContainersConfig;
import org.example.userservice.database.entity.User;
import org.example.userservice.exception.UserServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.example.userservice.fixture.UserTestFactory.createUser;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.DEFAULT_USER_NAME;
import static org.example.userservice.fixture.UserTestFactory.NON_EXISTENT_ID;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_AGE;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_EMAIL;
import static org.example.userservice.fixture.UserTestFactory.OTHER_USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class UserDaoImplTest extends TestContainersConfig {

    private UserDaoImpl userDao;

    @BeforeEach
    void setUp() {
        userDao = new UserDaoImpl(sessionFactory);
    }

    @Test
    @DisplayName("create: после сохранения у пользователя должен быть сгенерирован id")
    void create_validUser_shouldGenerateId() {
        // given
        User newUser = createUser(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);

        // when
        User savedUser = userDao.create(newUser);

        // then
        assertNotNull(savedUser.getId());
    }

    @Test
    @DisplayName("create: имя сохранённого пользователя должно совпадать с переданным")
    void create_validUser_shouldPersistName() {
        // given
        User newUser = createUser(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE);

        // when
        User savedUser = userDao.create(newUser);
        User persistedUser = userDao.findById(savedUser.getId()).orElseThrow();

        // then
        assertEquals(DEFAULT_USER_NAME, persistedUser.getName());
    }

    @Test
    @DisplayName("create: повторное сохранение с уже существующим email должно бросить UserServiceException")
    void create_duplicateEmail_shouldThrowUserServiceException() {
        // given
        userDao.create(createUser(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE));
        User duplicateEmailUser = createUser(OTHER_USER_NAME, DEFAULT_USER_EMAIL, OTHER_USER_AGE);

        // when / then
        assertThrows(UserServiceException.class, () -> userDao.create(duplicateEmailUser));
    }

    @Test
    @DisplayName("create: после ошибки дублирующегося email в базе не должно появиться лишней записи")
    void create_duplicateEmail_shouldNotLeaveOrphanRecord() {
        // given
        userDao.create(createUser(OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE));
        User duplicateEmailUser = createUser(OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE);

        // when
        assertThrows(UserServiceException.class, () -> userDao.create(duplicateEmailUser));

        // then
        assertEquals(1, userDao.findAll().size());
    }

    @Test
    @DisplayName("findById: для существующего id должен вернуться Optional с пользователем")
    void findById_existingId_shouldReturnPresentOptional() {
        // given
        User created = userDao.create(createUser(OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE));

        // when
        Optional<User> found = userDao.findById(created.getId());

        // then
        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("findById: найденный пользователь должен содержать корректное имя")
    void findById_existingId_shouldReturnUserWithCorrectName() {
        // given
        User created = userDao.create(createUser(OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE));

        // when
        Optional<User> found = userDao.findById(created.getId());

        // then
        assertEquals(OTHER_USER_NAME, found.orElseThrow().getName());
    }

    @Test
    @DisplayName("findById: для несуществующего id должен вернуться пустой Optional")
    void findById_nonExistentId_shouldReturnEmptyOptional() {
        // given / when
        Optional<User> found = userDao.findById(NON_EXISTENT_ID);

        // then
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("findAll: должен вернуть всех сохранённых пользователей")
    void findAll_multipleUsersPersisted_shouldReturnAllOfThem() {
        // given
        userDao.create(createUser(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE));
        userDao.create(createUser(OTHER_USER_NAME, OTHER_USER_EMAIL, OTHER_USER_AGE));

        // when
        List<User> allUsers = userDao.findAll();

        // then
        assertEquals(2, allUsers.size());
    }

    @Test
    @DisplayName("findAll: если пользователей нет, должен вернуть пустой список")
    void findAll_noUsersPersisted_shouldReturnEmptyList() {
        // given / when
        List<User> allUsers = userDao.findAll();

        // then
        assertTrue(allUsers.isEmpty());
    }

    @Test
    @DisplayName("update: изменённое имя должно сохраниться в базе")
    void update_existingUser_shouldPersistNewName() {
        // given
        User created = userDao.create(createUser(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE));
        created.setName(OTHER_USER_NAME);

        // when
        userDao.update(created);

        // then
        User persisted = userDao.findById(created.getId()).orElseThrow();
        assertEquals(OTHER_USER_NAME, persisted.getName());
    }

    @Test
    @DisplayName("deleteById: после удаления пользователь не должен находиться по id")
    void deleteById_existingId_shouldRemoveUser() {
        // given
        User created = userDao.create(createUser(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_USER_AGE));

        // when
        userDao.deleteById(created.getId());

        // then
        assertTrue(userDao.findById(created.getId()).isEmpty());
    }
}
