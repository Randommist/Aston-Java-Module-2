package org.example.notificationservice.kafka;

import jakarta.validation.ConstraintViolationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

/**
 * Сообщение, которое не удалось обработать, после нескольких попыток уходит
 * в топик {@code <topic>-dlt}, чтобы не блокировать чтение следующих сообщений.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    private static final long RETRY_INTERVAL_MS = 1000;
    private static final long MAX_RETRIES = 2;

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations);
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
        // Повтор не исправит битый JSON или невалидные данные — сразу в DLT.
        errorHandler.addNotRetryableExceptions(JacksonException.class, ConstraintViolationException.class);
        return errorHandler;
    }
}
