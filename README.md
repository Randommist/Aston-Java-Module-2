# User Service

Spring Boot-приложение с CRUD API для управления пользователями.

Сервис уведомлений находится в
[notification-service](notification-service/README.md). Он получает события
из Kafka и отправляет письма; публикация событий из `user-service` будет
добавлена отдельным этапом.

## Стек

- Java 17, Spring Boot 4
- Spring Web, Spring Data JPA, Bean Validation
- PostgreSQL
- OpenAPI 3 / Swagger UI
- JUnit 5, Mockito, MockMvc

## Запуск

Скопируйте `.env.example` в `.env`, затем запустите приложение и PostgreSQL:

```bash
docker compose up --build
```

При локальном запуске база по умолчанию ожидается по адресу
`jdbc:postgresql://localhost:5432/user_service` с логином и паролем `postgres`.
Параметры можно переопределить переменными `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` и `SPRING_DATASOURCE_PASSWORD`.

## API

| Метод | URL | Описание |
|---|---|---|
| `POST` | `/api/users` | Создать пользователя |
| `GET` | `/api/users` | Получить всех пользователей |
| `GET` | `/api/users/{id}` | Получить пользователя по id |
| `PUT` | `/api/users/{id}` | Обновить пользователя |
| `DELETE` | `/api/users/{id}` | Удалить пользователя |

Пример тела запроса для создания и обновления:

```json
{
  "name": "John",
  "email": "john@example.com",
  "age": 30
}
```

Entity из контроллера не возвращается: API использует request DTO и `UserResponse`.

После запуска доступны:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

## Тесты

```bash
mvn test
```

CRUD-контракт, статусы, валидация и ошибки API покрыты MockMvc-тестами.
