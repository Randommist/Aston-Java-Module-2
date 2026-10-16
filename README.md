# User Service

Spring Boot-приложение с CRUD API для управления пользователями и набор
сервисов вокруг него на Spring Cloud.

## Архитектура

```
 клиент ──► api-gateway :8080 ──► user-service :8082 ──Kafka──► notification-service :8081
               │  circuit breaker        │                              │
               ▼                         ▼                              ▼
       discovery-server :8761  ◄── регистрация сервисов ────────────────┘
       config-server    :8888  ──► настройки при старте всех сервисов
```

| Модуль | Порт | Паттерн |
|---|---|---|
| [`api-gateway`](api-gateway) | 8080 | API Gateway (Spring Cloud Gateway) + Circuit Breaker (Resilience4j) |
| [`discovery-server`](discovery-server) | 8761 | Service Discovery (Eureka) |
| [`config-server`](config-server) | 8888 | External Configuration (Spring Cloud Config) |
| `user-service` (корень репозитория) | 8082 | CRUD API пользователей, публикует события в Kafka |
| [`notification-service`](notification-service/README.md) | 8081 | Письма о создании и удалении пользователя |

- **API Gateway.** Клиенты ходят только на `http://localhost:8080`:
  `/api/users/**` уходит в user-service, `/api/notifications/**` — в
  notification-service. Адреса сервисов gateway берёт из Eureka.
- **Service Discovery.** Сервисы при старте регистрируются в Eureka под своим
  `spring.application.name`. Зарегистрированные сервисы видны на
  <http://localhost:8761>.
- **Circuit Breaker.** Если сервис недоступен или отвечает 502/503/504, gateway
  сразу возвращает `503` с описанием ошибки. После 50% сбоев (из последних 10
  вызовов, минимум 5) предохранитель размыкается на 10 секунд и запросы к
  сервису не отправляются вовсе.
- **External Configuration.** Настройки сервисов лежат в
  [`config-server/src/main/resources/config`](config-server/src/main/resources/config):
  `<сервис>.yml` для каждого сервиса и общий `application.yml`. Локально у
  сервиса только имя и адрес config-server. Секретов в конфигах нет: пароли и
  ключи передаются переменными окружения.

## Стек

- Java 17, Spring Boot 4.1, Spring Cloud 2025.1
- Spring Web, Spring Data JPA, Bean Validation
- Spring Cloud Gateway, Netflix Eureka, Spring Cloud Config, Resilience4j
- PostgreSQL, Kafka
- OpenAPI 3 / Swagger UI
- Spring HATEOAS (HAL)
- JUnit 5, Mockito, MockMvc

## Запуск

Скопируйте `.env.example` в `.env` и задайте в нём `NOTIFICATION_API_KEY`,
затем запустите всю систему:

```bash
docker compose up --build
```

Сервисы ждут, пока поднимутся config-server и discovery-server. После
регистрации в Eureka gateway начинает направлять на них запросы — это занимает
до 30 секунд после старта, до этого gateway отвечает `503`.

### Локальный запуск без Docker

Порядок важен: сервисы при старте обращаются к config-server и не запустятся
без него.

1. Инфраструктура: `docker compose up -d db kafka mailpit`.
2. `config-server` и `discovery-server`: `mvn spring-boot:run` в каталоге модуля.
3. `user-service` (`mvn spring-boot:run` в корне), `notification-service`
   (с переменной `NOTIFICATION_API_KEY`) и `api-gateway`.

Адреса config-server и Eureka меняются переменными `CONFIG_SERVER_URL` и
`EUREKA_URL`. Подключение к базе по умолчанию —
`jdbc:postgresql://localhost:5432/user_service` с логином и паролем `postgres`,
его можно переопределить переменными `SPRING_DATASOURCE_URL`,
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

### HATEOAS

Примеры ниже — через gateway. Ответы приходят в формате HAL (`application/hal+json`): кроме данных, в них есть
блок `_links` со ссылками на доступные действия.

```json
{
  "id": 1,
  "name": "John",
  "email": "john@example.com",
  "age": 30,
  "createdAt": "2026-01-01T12:00:00",
  "_links": {
    "self": { "href": "http://localhost:8080/api/users/1" },
    "update": { "href": "http://localhost:8080/api/users/1" },
    "delete": { "href": "http://localhost:8080/api/users/1" },
    "users": { "href": "http://localhost:8080/api/users" }
  }
}
```

`GET /api/users` возвращает пользователей в `_embedded.users`, у каждого свои
ссылки, плюс `_links.self` на сам список.

После запуска доступны:

- Swagger UI: <http://localhost:8082/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8082/v3/api-docs>

Swagger открывается напрямую на user-service, без gateway.

## Тесты

```bash
mvn test
```

CRUD-контракт, статусы, валидация и ошибки API покрыты MockMvc-тестами.
Тесты каждого модуля запускаются из его каталога. Config-server и Eureka для
тестов не нужны: сервисы читают свои файлы конфигурации из `config-server`
прямо с диска.
