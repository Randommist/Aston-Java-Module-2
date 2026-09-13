package config;

import lombok.extern.slf4j.Slf4j;
import org.example.userservice.database.entity.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Базовая конфигурация для интеграционных тестов DAO-слоя.
 * Поднимает контейнер PostgreSQL через Testcontainers, создаёт
 * {@link SessionFactory} и очищает данные перед каждым тестом,
 * чтобы тесты были независимы друг от друга.
 */
@Slf4j
@Testcontainers
public abstract class TestContainersConfig {

    private static final String PROPERTIES_FILE = "hibernate-test.properties";

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("userdb_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    protected static SessionFactory sessionFactory;

    @BeforeAll
    static void setUpSessionFactory() throws IOException {
        Properties properties = loadTestProperties();

        properties.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        properties.setProperty("hibernate.connection.url", POSTGRES_CONTAINER.getJdbcUrl());
        properties.setProperty("hibernate.connection.username", POSTGRES_CONTAINER.getUsername());
        properties.setProperty("hibernate.connection.password", POSTGRES_CONTAINER.getPassword());

        sessionFactory = new Configuration()
                .addProperties(properties)
                .addAnnotatedClass(User.class)
                .buildSessionFactory();

        log.info("Тестовая SessionFactory создана, контейнер: {}", POSTGRES_CONTAINER.getJdbcUrl());
    }

    private static Properties loadTestProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = TestContainersConfig.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalStateException(PROPERTIES_FILE + " не найден в classpath");
            }
            properties.load(input);
        }
        return properties;
    }

    @BeforeEach
    void resetDatabase() {
        try (var session = sessionFactory.openSession()) {
            var tx = session.beginTransaction();
            session.createMutationQuery("delete from User").executeUpdate();
            tx.commit();
        }
    }

    @AfterAll
    static void tearDownSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}