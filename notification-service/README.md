# Notification Service

Отправляет письма при создании и удалении пользователя. Событие приходит из
Kafka-топика `user-lifecycle`; то же действие доступно напрямую по HTTP.

## Локальный запуск через Docker

Kafka общая для обоих сервисов и описана в корневом `docker-compose.yaml`.
Из корня репозитория запустите её, затем из каталога `notification-service` —
тестовый SMTP-сервер и сам сервис:

```bash
docker compose up -d kafka
cd notification-service
docker compose -f compose.yaml up -d
NOTIFICATION_API_KEY=local-dev-key mvn spring-boot:run
```

Переменная `NOTIFICATION_API_KEY` обязательна: без неё сервис не запустится.

Сервис слушает `http://localhost:8081`. Mailpit показывает перехваченные письма
на <http://localhost:8025>. Для настоящего SMTP задайте `SPRING_MAIL_HOST`,
`SPRING_MAIL_PORT` и при необходимости стандартные свойства `spring.mail.*`.

## HTTP API

```bash
curl -i -X POST http://localhost:8081/api/notifications \
  -H 'X-API-Key: local-dev-key' \
  -H 'Content-Type: application/json' \
  -d '{"operation":"CREATED","email":"user@example.com"}'
```

Запросы без заголовка `X-API-Key` или с неверным ключом получают
`401 Unauthorized`. Успешная отправка возвращает `204 No Content`. Допустимые операции — `CREATED`
и `DELETED`. Сообщение Kafka в топике `user-lifecycle` использует такой же JSON:

```json
{"operation":"DELETED","email":"user@example.com"}
```

Если сообщение не удалось обработать, сервис повторяет попытку два раза с
интервалом в секунду, а затем перекладывает его в топик `user-lifecycle-dlt`
и читает дальше. Битый JSON и невалидные данные уходят туда сразу, без повторов.

`user-service` публикует эти события после коммита транзакции создания и
удаления пользователя.

## Тесты

```bash
mvn test
```

Интеграционные тесты запускают встроенную Kafka и тестовый SMTP-сервер GreenMail;
внешние сервисы для них не требуются.
