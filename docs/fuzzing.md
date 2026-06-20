# Фаззинг-тестирование OWASP ZAP

Фаззинг-тестирование применяется для проверки устойчивости backend API и
frontend к некорректным, случайным и потенциально вредоносным входным
данным, а также для выявления типовых проблем конфигурации (заголовки
безопасности, раскрытие служебной информации, доступ без авторизации).

## Воспроизводимый запуск (рекомендуемый способ)

Сканирование автоматизировано скриптом [`scripts/zap-scan.sh`](../scripts/zap-scan.sh)
и не требует ручных действий в UI ZAP. Скрипт:

1. Логинится тестовым администратором (`admin@example.com` / `admin12345`
   по умолчанию) и получает JWT — это позволяет ZAP сканировать защищённые
   эндпоинты, а не только публичные.
2. Прогоняет `zap-baseline.py` (образ `ghcr.io/zaproxy/zaproxy:stable`)
   отдельно против backend API и отдельно против frontend — так же, как
   зафиксировано в отчёте по проекту (два хоста: REST API и статический
   клиент).
3. Сохраняет HTML и JSON отчёты в [`docs/zap/reports/`](zap/reports) с
   именем вида `zap-api-<timestamp>.html` / `zap-frontend-<timestamp>.html`.

Сами отчёты не хранятся в git (см. `.gitignore` — каталог содержит только
`.gitkeep`), так как они должны переcоздаваться при каждом запуске, а не
быть «фотографией на память». Воспроизводимый артефакт — это сам скрипт,
конфигурация запуска и CI workflow, а не разово сделанный HTML-файл.

### Локальный запуск

```bash
docker compose up --build -d
bash scripts/zap-scan.sh
```

Переменные окружения (необязательно):

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `API_URL` | `http://localhost:8080` | адрес backend |
| `FRONTEND_URL` | `http://localhost:3000` | адрес frontend |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@example.com` / `admin12345` | учётная запись для авторизованного скана |
| `ZAP_IMAGE` | `ghcr.io/zaproxy/zaproxy:stable` | образ ZAP |

### Запуск в CI

Workflow [`.github/workflows/zap-scan.yml`](../.github/workflows/zap-scan.yml)
поднимает приложение через `docker compose`, дожидается готовности backend
(`/api/health`) и frontend, выполняет `scripts/zap-scan.sh` и публикует
получившиеся отчёты как артефакт прогона (`zap-reports`). Запускается:

- вручную — кнопка **Run workflow** в разделе Actions (`workflow_dispatch`);
- по расписанию — каждый понедельник в 03:00 UTC.

Скачать отчёт конкретного прогона можно со страницы соответствующего run
в GitHub Actions (вкладка Artifacts).

## Авторизация запросов

Скрипт автоматически добавляет JWT в заголовок через ZAP Replacer
(`-config replacer.full_list(0)...`), полученный логином тестового
администратора. Для ручной проверки в UI ZAP заголовок можно выставить
так же вручную:

```text
Authorization: Bearer <TOKEN>
```

## Проверяемые эндпоинты

- `POST /api/auth/register`;
- `POST /api/auth/login`;
- `GET /api/requests`, `POST /api/requests`, `PUT /api/requests/{id}`;
- `PATCH /api/requests/{id}/status`, `/assign`, `/classify`;
- `GET/POST /api/requests/{id}/comments`, `/attachments`;
- `GET /api/categories`, `/api/masters`, `/api/admin/users`.

## Примеры некорректных данных

```json
{
  "title": "",
  "description": "<script>alert(1)</script>",
  "equipmentType": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "priority": "CRITICAL"
}
```

Ожидаемый результат: сервер возвращает контролируемые ошибки `400`, `401`,
`403`, но не завершает работу аварийно и не раскрывает стек-трейс.

## Интерпретация результатов

`zap-baseline.py` запускается с флагом `-I` (предупреждения не приводят к
ошибке скрипта) — фаззинг-сканирование носит информационный характер и не
блокирует сборку. Уязвимости High/Critical в отчёте — повод завести задачу
на исправление; Medium/Low (например, отсутствие `Content-Security-Policy`
или хранение JWT в `localStorage`) — зафиксировать как известные ограничения
и приоритизировать отдельно.
