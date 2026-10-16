package org.example.notificationservice;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.notificationservice.controller.ApiKeyFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "notification.api.key=" + NotificationIntegrationTest.API_KEY)
@EmbeddedKafka(topics = "user-lifecycle", partitions = 1)
@DirtiesContext
class NotificationIntegrationTest {

    static final String API_KEY = "test-api-key";

    private static final GreenMail SMTP = new GreenMail(
            new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP)
    );

    static {
        SMTP.start();
    }

    @DynamicPropertySource
    static void smtpProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> SMTP.getSmtp().getPort());
    }

    @AfterAll
    static void stopSmtp() {
        SMTP.stop();
    }

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(applicationContext.getBean(ApiKeyFilter.class))
                .build();
    }

    @Test
    void httpApiSendsCreationEmail() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/notifications")
                        .header("X-API-Key", API_KEY)
                        .contentType("application/json")
                        .content("{\"operation\":\"CREATED\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isNoContent());

        MimeMessage message = awaitEmail(email);
        assertThat(message.getSubject()).isEqualTo("Аккаунт создан");
        assertThat(message.getContent().toString())
                .isEqualTo("Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.");
    }

    @Test
    void kafkaEventSendsDeletionEmail() throws Exception {
        String email = uniqueEmail();
        kafkaTemplate.send("user-lifecycle", "{\"operation\":\"DELETED\",\"email\":\"" + email + "\"}")
                .get(10, TimeUnit.SECONDS);

        MimeMessage message = awaitEmail(email);
        assertThat(message.getSubject()).isEqualTo("Аккаунт удалён");
        assertThat(message.getContent().toString())
                .isEqualTo("Здравствуйте! Ваш аккаунт был удалён.");
    }

    @Test
    void kafkaEventSendsCreationEmail() throws Exception {
        String email = uniqueEmail();
        kafkaTemplate.send("user-lifecycle", "{\"operation\":\"CREATED\",\"email\":\"" + email + "\"}")
                .get(10, TimeUnit.SECONDS);

        MimeMessage message = awaitEmail(email);
        assertThat(message.getSubject()).isEqualTo("Аккаунт создан");
        assertThat(message.getContent().toString())
                .isEqualTo("Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.");
    }

    @Test
    void httpApiRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .header("X-API-Key", API_KEY)
                        .contentType("application/json")
                        .content("{\"operation\":\"CREATED\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void httpApiRejectsMissingApiKey() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType("application/json")
                        .content("{\"operation\":\"CREATED\",\"email\":\"" + uniqueEmail() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void httpApiRejectsWrongApiKey() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .header("X-API-Key", "wrong-key")
                        .contentType("application/json")
                        .content("{\"operation\":\"CREATED\",\"email\":\"" + uniqueEmail() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedKafkaEventGoesToDeadLetterTopicAndDoesNotBlockNextEvents() throws Exception {
        String malformed = "not a json " + UUID.randomUUID();
        kafkaTemplate.send("user-lifecycle", malformed).get(10, TimeUnit.SECONDS);

        String email = uniqueEmail();
        kafkaTemplate.send("user-lifecycle", "{\"operation\":\"CREATED\",\"email\":\"" + email + "\"}")
                .get(10, TimeUnit.SECONDS);

        awaitEmail(email);
        assertThat(awaitDeadLetter(malformed)).isNotNull();
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.test";
    }

    private ConsumerRecord<String, String> awaitDeadLetter(String value) {
        var props = KafkaTestUtils.consumerProps(embeddedKafka, "dlt-" + UUID.randomUUID(), false);
        props.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(),
                new StringDeserializer()).createConsumer()) {
            consumer.subscribe(List.of("user-lifecycle-dlt"));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                    if (value.equals(record.value())) {
                        return record;
                    }
                }
            }
        }
        return fail("Сообщение не попало в user-lifecycle-dlt за 10 секунд");
    }

    private static MimeMessage awaitEmail(String email) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            for (MimeMessage message : SMTP.getReceivedMessages()) {
                if (Arrays.stream(message.getAllRecipients())
                        .anyMatch(recipient -> recipient.toString().equals(email))) {
                    return message;
                }
            }
            Thread.sleep(100);
        }
        return fail("Письмо для " + email + " не пришло за 10 секунд");
    }
}
