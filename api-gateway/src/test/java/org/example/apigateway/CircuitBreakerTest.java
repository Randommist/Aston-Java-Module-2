package org.example.apigateway;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * user-service «зарегистрирован» по адресу, где никто не слушает:
 * каждый вызов заканчивается ошибкой соединения.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CircuitBreakerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory;

    @DynamicPropertySource
    static void deadUserService(DynamicPropertyRegistry registry) throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].uri",
                () -> "http://127.0.0.1:" + freePort);
    }

    @Test
    void unavailableServiceReturnsFallbackAndOpensCircuit() {
        for (int i = 0; i < 5; i++) {
            client().post().uri("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Service Unavailable")
                    .jsonPath("$.service").isEqualTo("user-service");
        }

        CircuitBreaker circuitBreaker = circuitBreakerFactory.getCircuitBreakerRegistry()
                .circuitBreaker("user-service");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
