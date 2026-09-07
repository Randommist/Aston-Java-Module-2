package org.example.userservice.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private HibernateUtil() {
    }

    private static SessionFactory buildSessionFactory() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        return new Configuration()
                .configure()
                .setProperty("hibernate.connection.url", getRequiredConfig(dotenv, "DB_URL"))
                .setProperty("hibernate.connection.username", getRequiredConfig(dotenv, "DB_USERNAME"))
                .setProperty("hibernate.connection.password", getRequiredConfig(dotenv, "DB_PASSWORD"))
                .buildSessionFactory();
    }

    private static String getRequiredConfig(Dotenv dotenv, String key) {
        String value = System.getenv(key);

        if (value == null || value.isBlank()) {
            value = dotenv.get(key);
        }

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }

        return value;
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void shutdown() {
        SESSION_FACTORY.close();
    }
}
