#!/usr/bin/env python3
"""Проверка запущенной Compose-системы; только стандартная библиотека Python."""
import json
import os
import sys
import time
import uuid
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parent.parent
GATEWAY = os.getenv("GATEWAY_URL", "http://localhost:8080")
CONFIG = os.getenv("CONFIG_SERVER_URL", "http://localhost:8888")
DISCOVERY = os.getenv("DISCOVERY_URL", "http://localhost:8761")
MAILPIT = os.getenv("MAILPIT_URL", "http://localhost:8025")
TIMEOUT = int(os.getenv("SMOKE_TIMEOUT", "180"))


def api_key():
    value = os.getenv("NOTIFICATION_API_KEY")
    if value:
        return value
    env_file = ROOT / ".env"
    if env_file.exists():
        for line in env_file.read_text().splitlines():
            name, separator, value = line.strip().partition("=")
            if separator and name == "NOTIFICATION_API_KEY":
                return value.strip().strip("\"'")
    raise RuntimeError("Задайте NOTIFICATION_API_KEY в окружении или .env")


def request(url, method="GET", body=None, headers=None):
    data = None if body is None else json.dumps(body).encode()
    all_headers = {"Accept": "application/json"}
    if data is not None:
        all_headers["Content-Type"] = "application/json"
    all_headers.update(headers or {})
    req = Request(url, data=data, method=method, headers=all_headers)
    try:
        response = urlopen(req, timeout=10)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read().decode()
        return response.status, json.loads(raw) if raw else None


def expect(url, status=200, **kwargs):
    actual, body = request(url, **kwargs)
    assert actual == status, f"{kwargs.get('method', 'GET')} {url}: HTTP {actual}, ожидался {status}"
    return body


def wait_for(description, check, timeout=TIMEOUT):
    deadline = time.monotonic() + timeout
    last_error = None
    while time.monotonic() < deadline:
        try:
            result = check()
            if result:
                print(f"OK: {description}", flush=True)
                return result
        except (AssertionError, URLError, OSError, ValueError) as error:
            last_error = error
        time.sleep(2)
    raise TimeoutError(f"{description}: превышен таймаут {timeout}s; {last_error}")


def registry_ready():
    body = expect(f"{DISCOVERY}/eureka/apps")
    applications = body["applications"].get("application", [])
    expected = {"USER-SERVICE", "NOTIFICATION-SERVICE", "API-GATEWAY"}
    registered = {
        app["name"] for app in applications
        if any(instance["status"] == "UP" for instance in app["instance"])
    }
    return expected <= registered


def mail_received(email, subject, text):
    query = urlencode({"query": f"to:{email}"})
    body = expect(f"{MAILPIT}/api/v1/search?{query}")
    for message in body.get("messages", []):
        if message["Subject"] == subject:
            detail = expect(f"{MAILPIT}/api/v1/message/{message['ID']}")
            assert detail["Text"].strip() == text, "Неверный текст письма"
            assert any(recipient["Address"] == email for recipient in detail["To"])
            return True
    return False


def main():
    key = api_key()
    for service, address in [
        ("config-server", CONFIG),
        ("discovery-server", DISCOVERY),
        ("user-service", os.getenv("USER_SERVICE_URL", "http://localhost:8082")),
        ("notification-service", os.getenv("NOTIFICATION_SERVICE_URL", "http://localhost:8081")),
        ("api-gateway", GATEWAY),
    ]:
        wait_for(f"{service} готов", lambda url=address: (
            expect(f"{url}/actuator/health/readiness").get("status") == "UP"
        ))

    for service in ("user-service", "notification-service", "api-gateway"):
        config = expect(f"{CONFIG}/{service}/default")
        assert config["propertySources"], f"Config Server не вернул настройки {service}"
    print("OK: конфигурации трёх клиентов доступны через Config Server", flush=True)

    wait_for("три клиента зарегистрированы в Eureka", registry_ready)
    wait_for("Gateway маршрутизирует запросы к user-service",
             lambda: expect(f"{GATEWAY}/api/users") is not None)

    email = f"smoke-{uuid.uuid4().hex}@example.test"
    user_id = None
    try:
        created = expect(f"{GATEWAY}/api/users", status=201, method="POST",
                         body={"name": "Compose Smoke", "email": email, "age": 30})
        user_id = created["id"]
        user_url = f"{GATEWAY}/api/users/{user_id}"
        assert expect(user_url)["email"] == email
        assert created["_links"]["self"]["href"].startswith(GATEWAY)
        wait_for("создание → Kafka → письмо",
                 lambda: mail_received(email, "Аккаунт создан",
                     "Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан."))

        expect(user_url, method="PUT",
               body={"name": "Updated Smoke", "email": email, "age": 31})
        assert expect(user_url)["name"] == "Updated Smoke"
        users = expect(f"{GATEWAY}/api/users").get("_embedded", {}).get("users", [])
        assert any(user["id"] == user_id for user in users)
        expect(user_url, status=204, method="DELETE")
        user_id = None
        expect(user_url, status=404)
        wait_for("удаление → Kafka → письмо",
                 lambda: mail_received(email, "Аккаунт удалён",
                     "Здравствуйте! Ваш аккаунт был удалён."))

        command = {"operation": "CREATED", "email": f"api-{email}"}
        expect(f"{GATEWAY}/api/notifications", status=401, method="POST", body=command)
        expect(f"{GATEWAY}/api/notifications", status=204, method="POST",
               body=command, headers={"X-API-Key": key})
        wait_for("HTTP API уведомлений через Gateway → письмо",
                 lambda: mail_received(command["email"], "Аккаунт создан",
                     "Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан."))
    finally:
        if user_id is not None:
            expect(f"{GATEWAY}/api/users/{user_id}", status=204, method="DELETE")
    print("SMOKE TEST PASSED: конфигурация, discovery, Gateway, CRUD, Kafka и SMTP", flush=True)


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, RuntimeError, TimeoutError, URLError, OSError, ValueError, KeyError) as error:
        print(f"SMOKE TEST FAILED: {error}", file=sys.stderr)
        sys.exit(1)
