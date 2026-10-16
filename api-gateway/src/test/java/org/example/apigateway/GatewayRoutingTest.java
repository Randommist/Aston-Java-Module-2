package org.example.apigateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Вместо Eureka адреса сервисов задаются через simple discovery client,
 * сами сервисы заменены заглушками.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class GatewayRoutingTest {

    private static final StubService USER_SERVICE = stub("{\"service\":\"user-service\"}");
    private static final StubService NOTIFICATION_SERVICE = stub("{\"service\":\"notification-service\"}");

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].uri", USER_SERVICE::url);
        registry.add("spring.cloud.discovery.client.simple.instances.notification-service[0].uri",
                NOTIFICATION_SERVICE::url);
    }

    @AfterAll
    static void stopStubs() {
        USER_SERVICE.close();
        NOTIFICATION_SERVICE.close();
    }

    @Test
    void routesUsersToUserService() {
        client().get().uri("/api/users/1").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.service").isEqualTo("user-service");
    }

    @Test
    void routesNotificationsToNotificationService() {
        client().post().uri("/api/notifications").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.service").isEqualTo("notification-service");
    }

    @Test
    void preservesOriginalHostForHateoasLinks() {
        client().get().uri("/api/users").exchange().expectStatus().isOk();

        assertThat(USER_SERVICE.lastRequestHeader("Host")).isEqualTo("localhost:" + port);
    }

    @Test
    void unknownPathIsNotFound() {
        client().get().uri("/unknown").exchange().expectStatus().isNotFound();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private static StubService stub(String body) {
        try {
            return new StubService(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
