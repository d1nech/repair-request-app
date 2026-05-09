# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Fullstack CRUD application for repair requests. Spring Boot 3.3 (Java 17) backend with JWT auth + PostgreSQL, React/Vite/TypeScript frontend, packaged with Docker Compose. Russian-language UI/docs — preserve Russian strings when editing user-facing text.

## Common commands

Full stack via Docker (preferred — wires DB, backend, frontend, env vars together):

```bash
docker compose up --build      # http://localhost:3000 (UI), :8080/api, :8080/swagger-ui/index.html
docker compose down -v         # stop and wipe DB volume
```

Backend only (requires Postgres on :5432 — `docker compose up db`):

```bash
cd backend
mvn spring-boot:run            # runs on :8080
mvn -B test                    # full test suite (what CI runs)
mvn -Dtest=RepairRequestAppApplicationTests test   # single test class
```

Frontend only (talks to backend on :8080 via Vite proxy/`VITE_API_URL`):

```bash
cd frontend
npm install
npm run dev                    # http://localhost:5173
npm run build                  # tsc + vite build (CI runs this)
```

CI (`.github/workflows/ci.yml`) runs `mvn -B test` and `npm run build` on push/PR to `main`. There is no lint step — `npm run build` is the only frontend gate (TypeScript compilation).

## Architecture

### Backend (`backend/src/main/java/ru/mirea/repair/`)

Standard Spring Boot layered architecture: `controller/` → `service/` → `repository/` (Spring Data JPA) → `entity/`. DTOs in `dto/` mediate between controllers and services; **never expose JPA entities through controllers**.

Key cross-cutting concerns:

- **Security** (`security/`): stateless JWT. `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`, populates `SecurityContext` from `Authorization: Bearer ...`. `SecurityConfig` permits `/api/auth/**`, `/api/health`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`; restricts `/api/admin/**` to `ROLE_ADMIN`; everything else requires auth. `@EnableMethodSecurity` is on — service methods use `@PreAuthorize` for fine-grained checks (e.g., owner-or-admin on edit/delete).
- **Persistence**: `spring.jpa.hibernate.ddl-auto=validate` — schema is owned by **Flyway** migrations in `src/main/resources/db/migration/V*__*.sql`. Never let JPA auto-create tables; add a new `V<n>__*.sql` for any schema change.
- **Bootstrap data** (`config/`): test users `user@example.com / user12345` and `admin@example.com / admin12345` are seeded on first start. Don't rely on them in production.
- **Error handling** (`exception/`): a global `@RestControllerAdvice` returns a consistent error shape including a `validationErrors` map for `@Valid` failures — the frontend reads this in `getErrorMessage` (`frontend/src/api.ts:50`).

### Frontend (`frontend/src/`)

Deliberately minimal — single-component SPA. The whole UI lives in `App.tsx`; API calls are centralized in `api.ts`; shared types in `types.ts`. There is **no router, no state library, no component library** — don't add one without a clear reason. JWT is stored in `localStorage` and attached by an Axios interceptor.

`VITE_API_URL` chooses the API base; default `/api` works behind the Docker nginx (`frontend/nginx.conf`) which proxies to the backend service.

### Roles & domain

Two roles (`USER`, `ADMIN`) and five request statuses (`NEW`, `IN_PROGRESS`, `WAITING_PARTS`, `DONE`, `CANCELLED`). Status transitions are admin-only (`PATCH /api/requests/{id}/status`); USERs can only edit/delete their own requests. See README "Основные API-эндпоинты" for the full endpoint table.

## Configuration

Backend env vars (defaults in `application.yml`): `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` (must be ≥256 bits — the default dev value is intentionally long), `JWT_EXPIRATION_MS`, `ALLOWED_ORIGINS` (comma-separated CORS origins), `PORT`. Render deploy also accepts `DATABASE_URL`.

Frontend env: `VITE_API_URL`.

## Conventions

- Add a Flyway migration for every schema change. Don't edit existing `V*` files after they've been applied anywhere.
- New endpoints under `/api/admin/**` are auto-restricted to ADMIN by `SecurityConfig`; for other paths, add `@PreAuthorize` on the service method.
- Validate request DTOs with Bean Validation annotations + `@Valid` on the controller — the global advice already formats the response.
- Keep the frontend single-component shape unless the user asks to introduce routing/state management.
