# Клиент-серверное приложение обработки заявок на ремонт

Курсовой проект по дисциплине «Проектирование и разработка клиент-серверных приложений».

Проект представляет собой fullstack CRUD-приложение для регистрации, обработки и контроля заявок на ремонт. Система поддерживает регистрацию и вход пользователей, ролевую модель USER/ADMIN, работу с заявками, изменение статусов, хранение данных в PostgreSQL и запуск в Docker.

## Стек технологий

**Frontend:** React, TypeScript, Vite, Axios  
**Backend:** Java 17, Spring Boot, Spring Security, Spring Data JPA, JWT, Flyway  
**Database:** PostgreSQL 16  
**Инфраструктура:** Docker, Docker Compose, GitHub Actions  
**Тестирование:** JUnit, MockMvc, OWASP ZAP для фаззинг-тестирования

## Возможности

- регистрация пользователя;
- аутентификация по JWT;
- создание заявки на ремонт;
- просмотр собственных заявок;
- редактирование заявки владельцем;
- удаление заявки владельцем или администратором;
- изменение статуса заявки администратором;
- просмотр всех заявок администратором;
- базовая валидация входных данных;
- обработка ошибок API;
- автоматическое создание тестовых пользователей при первом запуске.

## Роли

| Роль | Возможности |
|---|---|
| USER | Создание и просмотр собственных заявок, редактирование и удаление своих заявок |
| ADMIN | Просмотр всех заявок, изменение статусов, удаление заявок, просмотр пользователей |

## Тестовые учетные записи

После первого запуска автоматически создаются тестовые пользователи:

| Логин | Пароль | Роль |
|---|---|---|
| user@example.com | user12345 | USER |
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
│   └── render-deploy.md
├── .github/workflows/ci.yml
├── docker-compose.yml
└── README.md
```

## Основные API-эндпоинты

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| POST | /api/auth/register | public | регистрация |
| POST | /api/auth/login | public | вход |
| GET | /api/requests | USER/ADMIN | список заявок |
| POST | /api/requests | USER/ADMIN | создание заявки |
| GET | /api/requests/{id} | USER/ADMIN | получение заявки |
| PUT | /api/requests/{id} | USER/ADMIN | обновление заявки |
| PATCH | /api/requests/{id}/status | ADMIN | изменение статуса |
| DELETE | /api/requests/{id} | USER/ADMIN | удаление заявки |
| GET | /api/admin/users | ADMIN | список пользователей |

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

## Фаззинг-тестирование

Инструкция по запуску OWASP ZAP приведена в файле `docs/fuzzing.md`.

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
