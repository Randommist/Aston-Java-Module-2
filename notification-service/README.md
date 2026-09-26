# Notification Service

Отправляет письма при создании и удалении пользователя. Событие приходит из
Kafka-топика `user-lifecycle`; то же действие доступно напрямую по HTTP.

## Локальный запуск через Docker

Из каталога `notification-service` запустите Kafka и тестовый SMTP-сервер:

```bash
docker compose -f compose.yaml up -d
mvn spring-boot:run
```

Сервис слушает `http://localhost:8081`. Mailpit показывает перехваченные письма
на <http://localhost:8025>. Для настоящего SMTP задайте `SPRING_MAIL_HOST`,
`SPRING_MAIL_PORT` и при необходимости стандартные свойства `spring.mail.*`.

## HTTP API

```bash
curl -i -X POST http://localhost:8081/api/notifications \
  -H 'Content-Type: application/json' \
  -d '{"operation":"CREATED","email":"user@example.com"}'
```

Успешная отправка возвращает `204 No Content`. Допустимые операции — `CREATED`
и `DELETED`. Сообщение Kafka в топике `user-lifecycle` использует такой же JSON:

```json
{"operation":"DELETED","email":"user@example.com"}
```

`user-service` пока не публикует эти события: его подключение будет следующим
этапом. Адрес пользователя при удалении нужно прочитать до удаления записи.

## Тесты

```bash
mvn test
```

Интеграционные тесты запускают встроенную Kafka и тестовый SMTP-сервер GreenMail;
внешние сервисы для них не требуются.
