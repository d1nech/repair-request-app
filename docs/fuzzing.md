# Фаззинг-тестирование OWASP ZAP

Фаззинг-тестирование применяется для проверки устойчивости API к некорректным, случайным и потенциально вредоносным входным данным.

## Запуск приложения

```bash
docker compose up --build
```

## Получение JWT-токена

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin12345"}'
```

Скопируйте значение поля `token`.

## Быстрая проверка ZAP Baseline Scan

```bash
docker run --rm -t --network host ghcr.io/zaproxy/zaproxy:stable zap-baseline.py \
  -t http://localhost:8080 \
  -r zap-report.html
```

## Ручная проверка API

В OWASP ZAP можно добавить заголовок авторизации:

```text
Authorization: Bearer <TOKEN>
```

Далее проверяются основные эндпоинты:

- `POST /api/auth/register`;
- `POST /api/auth/login`;
- `GET /api/requests`;
- `POST /api/requests`;
- `PUT /api/requests/{id}`;
- `PATCH /api/requests/{id}/status`.

## Примеры некорректных данных

```json
{
  "title": "",
  "description": "<script>alert(1)</script>",
  "equipmentType": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "priority": "CRITICAL"
}
```

Ожидаемый результат: сервер возвращает контролируемые ошибки `400`, `401`, `403`, но не завершает работу аварийно и не раскрывает стек-трейс.
