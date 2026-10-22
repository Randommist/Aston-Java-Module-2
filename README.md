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

## Запуск всей системы через Docker Compose

Нужны Docker с Compose v2 и примерно 6 ГБ свободной оперативной памяти.
Java и Maven на хосте не требуются: все пять приложений собираются внутри
многостадийных Dockerfile. Для smoke-теста нужен Python 3 без дополнительных пакетов.

```bash
cp .env.example .env
# Укажите свой NOTIFICATION_API_KEY в .env.
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
python3 scripts/smoke_test.py
```

[`docker-compose.yml`](docker-compose.yml) запускает восемь контейнеров:
PostgreSQL, Kafka в режиме KRaft (без ZooKeeper), Mailpit и пять Spring-приложений.
Проверки готовности ожидают PostgreSQL, доступность Kafka и SMTP, а у Spring-сервисов
проверяют Actuator `/actuator/health/readiness`. Зависимости запускаются раньше
клиентов; Gateway стартует после готовности обоих микросервисов.
Регистрация и обновление кэша Eureka могут занять ещё до минуты: smoke-тест
ожидает регистрации и доступности маршрутов.

Внутри контейнеров используются адреса `db:5432`, `kafka:29092`, `mailpit:1025`,
`config-server:8888`, `discovery-server:8761`. Kafka также публикует отдельный
listener `localhost:9092` для клиентов с хоста. Имена контейнеров и томов
изолированы проектом Compose; PostgreSQL и Kafka сохраняют данные в именованных томах.

### Проверка взаимодействия контейнеров

`python3 scripts/smoke_test.py` проверяет:

- готовность пяти приложений и выдачу конфигураций через Config Server;
- регистрацию user-service, notification-service и Gateway в Eureka;
- создание, чтение, обновление и удаление пользователя через Gateway;
- фактическую доставку писем о создании и удалении через Kafka и SMTP;
- HTTP API уведомлений через Gateway: `401` без ключа, `204` с ключом и доставку письма.

Тест использует уникальные email и удаляет созданного пользователя. Чужие записи
и письма не очищаются. Ключ читается из окружения или `.env` и не выводится.
При успехе выводится `SMOKE TEST PASSED`, при ошибке — ненулевой код завершения.
Эта же проверка выполняется в GitHub Actions (`Compose integration`).

Полезные адреса:

| Адрес | Назначение |
|---|---|
| <http://localhost:8080/api/users> | API через Gateway |
| <http://localhost:8761> | Реестр Eureka |
| <http://localhost:8888/user-service/default> | Внешняя конфигурация user-service |
| <http://localhost:8025> | Письма в Mailpit (реальная рассылка отключена) |
| <http://localhost:8082/swagger-ui.html> | Swagger user-service |

Порты привязаны к `127.0.0.1`. Они должны быть свободны; это стенд для локальной
проверки, а не конфигурация публичного production-развёртывания.

```bash
docker compose logs -f user-service notification-service api-gateway
docker compose down
```

`down` останавливает систему, сохраняя данные PostgreSQL и Kafka. Для полного сброса
можно выполнить `docker compose down --volumes`: эта команда удалит данные стенда.

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
