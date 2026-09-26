package org.example.notificationservice;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@EmbeddedKafka(topics = "user-lifecycle", partitions = 1)
@DirtiesContext
class NotificationIntegrationTest {

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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void httpApiSendsCreationEmail() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/notifications")
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
                        .contentType("application/json")
                        .content("{\"operation\":\"CREATED\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.test";
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
