package org.example.apigateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ответ, который получает клиент, когда сервис недоступен или circuit breaker разомкнут.
 * Без метода в {@code @RequestMapping}: gateway пересылает сюда запрос с исходным методом.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/{service}")
    public ResponseEntity<ProblemDetail> serviceUnavailable(@PathVariable String service) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Сервис " + service + " временно недоступен, попробуйте позже");
        problem.setTitle("Service Unavailable");
        problem.setProperty("service", service);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
