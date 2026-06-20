# Быстрый деплой на Render

Проект разворачивается на Render тремя ресурсами:

1. PostgreSQL Database — база данных.
2. Web Service — backend Spring Boot через Dockerfile.
3. Static Site — frontend React/Vite.

## 1. Подготовка GitHub

Создайте публичный репозиторий `repair-request-app` и загрузите проект.

## 2. Создание PostgreSQL

Render Dashboard → New → PostgreSQL.

Рекомендуемые значения:

- Name: `repair-db`
- Database: `repair_db`
- User: `repair_user`
- Region: Frankfurt / ближайший регион
- Plan: Free

После создания откройте базу и скопируйте `Internal Database URL`.

## 3. Создание backend

Render Dashboard → New → Web Service → подключить GitHub-репозиторий.

Настройки:

- Name: `repair-request-backend`
- Runtime: Docker
- Root Directory: `backend`
- Branch: `main`
- Plan: Free
- Health Check Path: `/api/health`

Environment Variables:

- `DATABASE_URL` = Internal Database URL из PostgreSQL
- `JWT_SECRET` = длинная случайная строка не короче 32 символов
- `JWT_EXPIRATION_MS` = `86400000`
- `ALLOWED_ORIGINS` = временно `*` не использовать; после создания frontend указать его URL, например `https://repair-request-frontend.onrender.com`
- `APP_SEED_DEMO_DATA` = `true` — **только для этого учебного демо-деплоя**, чтобы
  на нём были доступны тестовые логины из раздела 5. Сидинг тестовых
  пользователей с известными паролями — это admin-процесс (Twelve-Factor App,
  фактор XII) и по умолчанию отключён (`false`); в реальном production-окружении
  эту переменную не выставлять.

После деплоя проверить:

- `https://repair-request-backend.onrender.com/api/health`
- `https://repair-request-backend.onrender.com/swagger-ui/index.html`

## 4. Создание frontend

Render Dashboard → New → Static Site → подключить тот же репозиторий.

Настройки:

- Name: `repair-request-frontend`
- Root Directory: `frontend`
- Build Command: `npm install && npm run build`
- Publish Directory: `dist`

Environment Variables:

- `VITE_API_URL` = `https://repair-request-backend.onrender.com/api`

После создания frontend вернуться в backend → Environment и поставить:

- `ALLOWED_ORIGINS` = `https://repair-request-frontend.onrender.com`

Затем нажать Manual Deploy → Deploy latest commit у backend.

## 5. Проверочные данные

Пользователь:

- `user@example.com`
- `user12345`

Администратор:

- `admin@example.com`
- `admin12345`

## 6. Что сфотографировать для отчета

1. Страница авторизации frontend.
2. Главная страница со списком заявок.
3. Создание новой заявки.
4. Редактирование/изменение статуса заявки.
5. Успешный `GET /api/health`.
6. Swagger UI.
7. `POST /api/auth/login` в Swagger/Postman.
8. `GET /api/requests` с Bearer Token.
9. Страница Render с успешным Deploy backend.
10. Страница Render с успешным Deploy frontend.
