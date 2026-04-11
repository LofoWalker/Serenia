# AGENTS.md — Serenia

## Overview

Serenia is an AI chat companion web app (French-language) with a freemium subscription model. Three-tier architecture: Angular 21 SPA → Quarkus 3.34 REST API → PostgreSQL 16. External integrations: OpenAI (chat), Stripe (payments), SMTP (email verification).

## Architecture

- **Backend**: Java 21 / Quarkus, package root `com.lofo.serenia` with layers: `rest/resource/` (JAX-RS endpoints) → `service/` (business logic, organized by domain: `chat/`, `user/`, `subscription/`, `mail/`, `admin/`) → `persistence/` (Panache repositories + JPA entities). DTOs live in `rest/dto/in/` (request records) and `rest/dto/out/` (response records). MapStruct mappers in `mapper/`.
- **Frontend**: Angular 21 standalone components (no NgModules), lazy-loaded routes in `app.routes.ts`. Structure: `core/` (services, guards, interceptors, models), `features/` (auth, chat, profile, admin, etc.), `shared/` (layouts, UI components). State managed via Angular signals (`signal()`, `computed()`), not NgRx.
- **Chat flow**: `ConversationResource` → `ChatOrchestrator.processUserMessage()` → `QuotaService` (check limits) → `MessageService` (encrypt & persist) → `ChatCompletionService` (OpenAI call) → encrypt assistant reply → return. Messages are AES-256-GCM encrypted per-user via HKDF key derivation in `EncryptionService`.
- **System prompt**: Loaded from `src/main/resources/prompt.md` at startup by `SystemPromptProvider`. The AI persona is a casual French friend, max 180 chars per reply.

## Dev Commands

```bash
# Backend (from backend/)
./mvnw quarkus:dev          # Dev mode with hot reload on :8080
./mvnw test                 # Unit + integration tests (H2 in PostgreSQL mode)
./mvnw test jacoco:report   # Tests + coverage (minimum 60% line coverage enforced)

# Frontend (from frontend/)
npm install
npm start                   # Dev server on :4200, proxies to :8080/api
npm test                    # Vitest
npm run lint                # ESLint
npm run format              # Prettier

# Full stack (from root)
docker compose up -d        # Production: Traefik + PostgreSQL + Backend + Frontend
```

## Backend Conventions

- **Config**: Use `@ConfigMapping` interfaces (see `SereniaConfig`, `OpenAIConfig`, `StripeConfig`), not `@ConfigProperty` fields. Prefix: `serenia.*`, `openai.*`, `stripe.*`.
- **DI**: Constructor injection via Lombok `@AllArgsConstructor` on `@ApplicationScoped` beans. No field `@Inject` except in tests.
- **DTOs**: Java records in `rest/dto/in/` and `rest/dto/out/`. Never reuse entities in REST responses.
- **Exceptions**: Global handler via `GlobalExceptionHandler` (JAX-RS `@Provider`). Delegates to `ExceptionHandlerService`. Custom exceptions in `exception/exceptions/`. Each error response includes a `traceId`.
- **Tests**: Integration tests use `@QuarkusTest` with H2 in PostgreSQL compatibility mode (config in `src/test/resources/application.properties`). REST tests use RestAssured against port `8081`. JWT tokens generated via `JwtTestTokenGenerator`. Test names use `should_describe_behavior` snake_case with `@DisplayName`.
- **DB migrations**: Liquibase YAML changelogs in `src/main/resources/db/changelog/`, numbered sequentially (`01-init-datamodel.yaml`, `02-plans-subscriptions.yaml`, etc.), registered in `changelog.xml`.
- **Style**: Google Java Style via checkstyle (`checkstyle.xml`), max 100 char line length, 2-space indent. Lombok + MapStruct annotation processors configured in `pom.xml`.
- **Security**: `@Authenticated` on resource classes. User ID extracted from JWT subject (`jwt.getSubject()` → UUID). Roles: `USER`, `ADMIN`.

## Frontend Conventions

- **State**: Angular signals pattern — services hold private `signal()`, expose `readonly` computed views. See `AuthStateService` and `ChatService` as reference implementations.
- **Auth**: JWT stored in `sessionStorage` (key: `serenia_token`). `authInterceptor` attaches `Bearer` header. Auto-redirect to `/login` on 401. Session restored at app init via `APP_INITIALIZER`.
- **API base URL**: Configured in `environments/environment.ts` as `apiUrl` (default: `http://localhost:8080/api`).
- **Styling**: TailwindCSS 4 with PostCSS. No component CSS modules.
- **Routing**: All feature components are lazy-loaded. `authGuard` / `guestGuard` for route protection, `adminGuard` for admin panel.
- **Testing**: Vitest (not Karma/Jasmine). Spec files colocated with source (`.spec.ts`).
- **Lint**: ESLint + Prettier enforced via husky pre-commit hooks (`lint-staged`).

## Key Files

| Purpose | Path |
|---|---|
| Backend entry config | `backend/src/main/resources/application.properties` |
| Test config overrides | `backend/src/test/resources/application.properties` |
| AI persona prompt | `backend/src/main/resources/prompt.md` |
| Encryption (AES-GCM + HKDF) | `backend/.../service/chat/EncryptionService.java` |
| Chat orchestration | `backend/.../service/chat/ChatOrchestrator.java` |
| Global error handling | `backend/.../exception/GlobalExceptionHandler.java` |
| Frontend routes | `frontend/src/app/app.routes.ts` |
| Frontend auth state | `frontend/src/app/core/services/auth-state.service.ts` |
| Docker production stack | `compose.yaml` |
| DB migrations | `backend/src/main/resources/db/changelog/` |

## Secrets & Environment

Secrets in production use Docker Secrets (files mounted at `/run/secrets/`). For local dev, set environment variables or use `.env`. Required secrets: `db_password`, `jwt_private_key.pem`, `jwt_public_key.pem`, `security_key`, `openai_api_key`, `stripe_secret_key`, `stripe_webhook_secret`, `smtp_password`. Dev JWT keys are in `backend/keys/`.

