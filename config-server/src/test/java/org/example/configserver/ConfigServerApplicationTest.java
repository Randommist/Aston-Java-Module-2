package org.example.configserver;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerApplicationTest {

    @LocalServerPort
    private int port;

    @ParameterizedTest
    @CsvSource({
            "user-service, spring.datasource.url",
            "notification-service, notification.mail.from",
            "api-gateway, resilience4j.circuitbreaker.configs.default.sliding-window-size"
    })
    void servesServiceAndSharedConfig(String service, String serviceProperty) throws Exception {
        String body = get("/" + service + "/default");

        assertThat(body)
                .contains("config/" + service + ".yml")
                .contains("config/application.yml")
                .contains(serviceProperty)
                .contains("eureka.client.service-url.defaultZone");
    }

    private String get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }
}
