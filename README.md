# Клиент-серверное приложение обработки заявок на ремонт

Курсовой проект по дисциплине «Проектирование и разработка клиент-серверных приложений».

Проект представляет собой fullstack CRUD-приложение для регистрации, обработки и контроля заявок на ремонт. Система поддерживает регистрацию и вход пользователей, ролевую модель USER/MASTER/OPERATOR/ADMIN, классификацию заявок по категориям, назначение мастера, комментарии и вложения к заявкам, историю изменения статусов, хранение данных в PostgreSQL и запуск в Docker.

## Стек технологий

**Frontend:** React, TypeScript, Vite, Axios  
**Backend:** Java 17, Spring Boot, Spring Security, Spring Data JPA, JWT, Flyway  
**Database:** PostgreSQL 16  
**Инфраструктура:** Docker, Docker Compose, GitHub Actions  
**Тестирование:** JUnit 5, Mockito (unit-тесты бизнес-логики сервисов), MockMvc + H2 (интеграционные тесты REST API и security-механизмов: роли, JWT, права доступа), OWASP ZAP для фаззинг-тестирования

## Возможности

- регистрация пользователя;
- аутентификация по JWT;
- создание заявки на ремонт;
- просмотр собственных заявок (клиент), назначенных заявок (мастер) или всех заявок (оператор/администратор);
- редактирование заявки владельцем;
- удаление заявки владельцем или администратором;
- классификация заявки по категории оператором/администратором;
- назначение мастера на заявку оператором/администратором;
- изменение статуса заявки администратором, оператором или назначенным мастером;
- комментирование заявки участниками, имеющими к ней доступ;
- прикрепление и скачивание файлов (вложений) к заявке;
- история изменения статусов заявки;
- просмотр всех заявок администратором и оператором;
- базовая валидация входных данных;
- обработка ошибок API;
- автоматическое создание тестовых пользователей при первом запуске.

## Роли

| Роль | Возможности |
|---|---|
| USER (клиент) | Создание заявки, просмотр и отслеживание статуса своих заявок, комментирование, редактирование и удаление своих заявок |
| MASTER (мастер) | Просмотр назначенных ему заявок, комментирование, изменение статуса и закрытие назначенной заявки |
| OPERATOR (оператор) | Просмотр всех заявок, классификация заявок по категориям, назначение мастера, изменение статуса, просмотр аналитики |
| ADMIN (администратор) | Просмотр всех заявок, управление пользователями и категориями, изменение статусов, удаление заявок |

## Тестовые учетные записи

При запуске через `docker compose up` (а также при явном
`APP_SEED_DEMO_DATA=true`, см. ниже) при первом старте создаются тестовые
пользователи:

| Логин | Пароль | Роль |
|---|---|---|
| user@example.com | user12345 | USER |
| master@example.com | master12345 | MASTER |
| operator@example.com | operator12345 | OPERATOR |
| admin@example.com | admin12345 | ADMIN |

## Быстрый запуск через Docker Compose

Требования:

- Docker Desktop;
- Docker Compose.

Команда запуска:

```bash
docker compose up --build
```

После запуска будут доступны:

- frontend: http://localhost:3000
- backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- PostgreSQL: localhost:5432

Остановка:

```bash
docker compose down
```

Остановка с удалением данных БД:

```bash
docker compose down -v
```

## Локальный запуск без Docker

### 1. Запуск базы данных

```bash
docker compose up db
```

### 2. Запуск backend

```bash
cd backend
mvn spring-boot:run
```

### 3. Запуск frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend будет доступен по адресу: http://localhost:5173

## Переменные окружения backend

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| SPRING_DATASOURCE_URL | jdbc:postgresql://localhost:5432/repair_db | строка подключения к БД |
| SPRING_DATASOURCE_USERNAME | repair_user | пользователь БД |
| SPRING_DATASOURCE_PASSWORD | repair_password | пароль БД |
| JWT_SECRET | local-dev-secret-local-dev-secret-local-dev-secret-123456 | секрет JWT |
| JWT_EXPIRATION_MS | 86400000 | срок действия токена |
| APP_UPLOAD_DIR | uploads | каталог хранения вложений заявок |
| APP_SEED_DEMO_DATA | false | сидинг тестовых пользователей и демо-заявок при старте (см. ниже) |

### Сидинг тестовых данных (APP_SEED_DEMO_DATA)

Создание тестовых пользователей (`user@example.com`, `admin@example.com`,
`master@example.com`, `operator@example.com`) и демо-заявок — это
admin-процесс в терминах [Twelve-Factor App](https://12factor.net/ru/admin-processes)
(фактор XII), поэтому он **не выполняется неявно при каждом старте
приложения**. По умолчанию `APP_SEED_DEMO_DATA=false`, и в production
сидинг не запускается.

Включить его явно нужно только в dev/demo-окружениях:

- `docker compose up --build` — включён по умолчанию (`APP_SEED_DEMO_DATA: "true"`
  в `docker-compose.yml`), поэтому тестовые логины из раздела
  «Тестовые учетные записи» доступны сразу после запуска;
- `mvn spring-boot:run` локально — по умолчанию выключен; чтобы получить
  тестовых пользователей, запустить с `APP_SEED_DEMO_DATA=true mvn spring-boot:run`;
- Render/реальный деплой — переменную не выставлять, если это не учебная
  демонстрация (см. `docs/render-deploy.md`).

## Структура проекта

```text
repair-request-app/
├── backend/
│   ├── src/main/java/ru/mirea/repair/
│   │   ├── config/          # начальные данные и CORS
│   │   ├── controller/      # REST-контроллеры
│   │   ├── dto/             # DTO запросов и ответов
│   │   ├── entity/          # JPA-сущности
│   │   ├── exception/       # обработка ошибок
│   │   ├── repository/      # доступ к данным
│   │   ├── security/        # JWT и Spring Security
│   │   └── service/         # бизнес-логика
│   ├── src/main/resources/db/migration/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── docs/
│   ├── fuzzing.md
│   ├── render-deploy.md
│   └── zap/reports/      # отчёты ZAP (генерируются, не хранятся в git)
├── scripts/
│   └── zap-scan.sh
├── .github/workflows/
│   ├── ci.yml
│   └── zap-scan.yml
├── docker-compose.yml
└── README.md
```

## Основные API-эндпоинты

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| POST | /api/auth/register | public | регистрация |
| POST | /api/auth/login | public | вход |
| GET | /api/requests | любая роль | список заявок (свои/назначенные/все — в зависимости от роли) |
| POST | /api/requests | USER/ADMIN | создание заявки |
| GET | /api/requests/{id} | владелец/назначенный мастер/OPERATOR/ADMIN | получение заявки |
| PUT | /api/requests/{id} | владелец/ADMIN | обновление заявки |
| PATCH | /api/requests/{id}/status | ADMIN/OPERATOR/MASTER (назначенный) | изменение статуса |
| PATCH | /api/requests/{id}/assign | ADMIN/OPERATOR | назначение мастера |
| PATCH | /api/requests/{id}/classify | ADMIN/OPERATOR | классификация по категории |
| DELETE | /api/requests/{id} | владелец/ADMIN | удаление заявки |
| GET/POST | /api/requests/{id}/comments | имеющие доступ к заявке | комментарии к заявке |
| GET/POST | /api/requests/{id}/attachments | имеющие доступ к заявке | вложения заявки |
| GET | /api/requests/{id}/attachments/{attachmentId}/download | имеющие доступ к заявке | скачивание вложения |
| GET | /api/requests/{id}/status-history | имеющие доступ к заявке | история статусов |
| GET | /api/categories | любая роль | список категорий |
| POST/PUT | /api/categories | ADMIN/OPERATOR | создание/изменение категории |
| DELETE | /api/categories/{id} | ADMIN | удаление категории |
| GET | /api/masters | ADMIN/OPERATOR | список пользователей с ролью MASTER |
| GET | /api/admin/users | ADMIN | список пользователей |
| POST | /api/admin/users | ADMIN | создание пользователя с произвольной ролью |

## Статусы заявок

- NEW — новая заявка;
- IN_PROGRESS — заявка принята в работу;
- WAITING_PARTS — ожидание деталей;
- DONE — ремонт выполнен;
- CANCELLED — заявка отменена.

## Проверка работоспособности

```bash
curl http://localhost:8080/api/health
```

## Автоматизированное тестирование backend

Тесты лежат в `backend/src/test/java/ru/mirea/repair/` и запускаются через `mvn -B test` (используют встроенную H2 в режиме совместимости с PostgreSQL — поднимать реальный PostgreSQL для тестов не нужно, конфигурация — `backend/src/test/resources/application.yml`):

- **Unit-тесты бизнес-логики** (`service/`, Mockito): `RepairRequestServiceTest` — матрица прав на смену статуса (ADMIN/OPERATOR — всегда, MASTER — только по своей назначенной заявке, USER — запрещено), назначение мастера (отказ, если роль не MASTER), классификация по категории, создание заявки с фиксацией в истории статусов; `AuthServiceTest` — конфликт при повторной регистрации email, корректная выдача токена, проброс ошибки при неверном пароле; `RequestAccessServiceTest` — матрица доступа на чтение/запись (владелец, ADMIN, OPERATOR, назначенный/не назначенный MASTER, посторонний USER).
- **Интеграционные тесты REST API и security** (MockMvc, полный Spring-контекст с реальным security filter chain): `AuthIntegrationTest` — регистрация, конфликт email (409), валидация (400), вход с верным/неверным паролем (200/401); `RepairRequestSecurityIntegrationTest` — блокировка запроса без JWT, видимость заявок по ролям (клиент видит только свои, оператор/админ — все), запрет доступа к чужой заявке и комментариям (403), разграничение смены статуса по ролям, сценарий «оператор классифицирует и назначает мастера → только назначенный мастер может закрыть заявку», отказ при назначении не-мастера (400), история статусов.

## Фаззинг-тестирование

Фаззинг-сканирование OWASP ZAP воспроизводимо: скрипт
[`scripts/zap-scan.sh`](scripts/zap-scan.sh) запускает `zap-baseline.py`
против backend API и frontend и сохраняет HTML/JSON отчёты в
`docs/zap/reports/`; тот же скрипт выполняется в CI workflow
[`zap-scan.yml`](.github/workflows/zap-scan.yml) (вручную через
`workflow_dispatch` или по расписанию), результат публикуется как артефакт
прогона в GitHub Actions. Подробности — в `docs/fuzzing.md`.

## Рекомендации по ведению GitHub-репозитория

Для курсовой работы желательно сделать не один коммит, а несколько логических коммитов:

```bash
git init
git add README.md .gitignore
git commit -m "docs: add project description"

git add backend
git commit -m "feat: add Spring Boot backend"

git add frontend
git commit -m "feat: add React frontend"

git add docker-compose.yml .github docs
git commit -m "chore: add docker compose, CI and fuzzing docs"
```

После этого репозиторий можно загрузить на GitHub.

## Развертывание в облаке Render

Для выполнения требования о размещении клиент-серверного приложения в облаке проект можно развернуть на Render.

Краткая схема:

1. Создать PostgreSQL Database на Render.
2. Создать Web Service для `backend` с Runtime = Docker и Root Directory = `backend`.
3. Создать Static Site для `frontend` с Root Directory = `frontend`.
4. В frontend указать переменную `VITE_API_URL` со ссылкой на backend API.
5. В backend указать переменные `DATABASE_URL`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `ALLOWED_ORIGINS`.

Подробная инструкция приведена в файле `docs/render-deploy.md`.

Проверочные URL после деплоя:

- `https://<backend-name>.onrender.com/api/health`
- `https://<backend-name>.onrender.com/swagger-ui/index.html`
- `https://<frontend-name>.onrender.com`
