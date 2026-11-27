# dVault

A modular monolith for private file storage, secure sharing and URL shortening.
The foundation provides a responsive React workspace, health endpoint, session CSRF contract and automated verification. Authentication and domain features are subsequent modules; the navigation currently shows release status rather than fabricated file or analytics data.

## Development setup

Requirements: Java 21, Node.js 24, PostgreSQL 17, and network access for the first dependency download. Maven 3.9.11 is downloaded by the committed Maven wrapper. Spring Boot is pinned to 3.5.16; React/TypeScript/Vite dependencies are pinned by `frontend/package-lock.json`.

Create a local PostgreSQL database and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in your shell using `.env.example` as a reference. The application does not automatically load `.env` files. Never commit real credentials. Set `COOKIE_SECURE=true` behind HTTPS in production.

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/dvault'
$env:DB_USERNAME='<your local database user>'
$env:DB_PASSWORD='<your local database password>'
cd frontend
npm ci
npm test
npm run build
cd ../backend
.\mvnw.cmd -B verify
.\mvnw.cmd spring-boot:run
```

On Linux/macOS use `export` for environment variables and `./mvnw` for Maven. For interactive frontend development run `npm run dev` inside `frontend`; Vite proxies `/api` and `/u` to port 8080. The frontend build is copied into the Spring Boot jar when built before Maven. Production uses the same origin; `java -jar backend/target/dvault-0.1.0-SNAPSHOT.jar` serves the built workspace at port 8080.

## Architecture and shared contracts

- Backend: Java 21 / Spring Boot, Spring Security, JPA, Flyway and PostgreSQL. Feature packages are `com.dvault.auth`, `user`, `vault`, `storage`, `sharing`, `shortener`, `analytics`, `admin`, `security`, and `common`. Add packages when implementing their features. Controllers call services; services own transactions and use repositories/adapters. Return DTOs, never entities or storage object keys.
- Frontend: React + TypeScript + Vite. `src/components.tsx` supplies labeled fields, a native focus-trapping dialog, loading and retryable error states. `src/api.ts` centralizes same-origin session requests and CSRF headers. No authentication tokens in localStorage.
- Database: PostgreSQL only; Hibernate validates the schema and Flyway manages changes. No domain tables exist yet. Rana assigns the next globally increasing migration version in actual merge order; never edit an applied migration. ERD and resource API details will be added with their implementing modules.
- Session security: CSRF protection stays enabled, session cookies are HttpOnly and SameSite=Lax, anonymous requests cannot read private APIs. Registration, login/logout and RBAC belong to the authentication module. Framework-generated development accounts are disabled; no login mechanism is enabled until the authentication module.
- Errors: subsequent resource controllers use `{status,code,message,path,timestamp,fieldErrors}`. `fieldErrors` maps field names to messages; timestamps use UTC ISO-8601. Validation is 400, authentication 401, authorization 403, missing resource 404, conflict 409, expired link 410, upload limit 413 and rate limit 429. Clients must also tolerate non-JSON proxy errors.

## Foundation API

| Method | Path | Response |
| --- | --- | --- |
| GET | `/api/health` | `200 {"status":"UP"}`; process health, no private details |
| GET | `/api/auth/csrf` | `200 {"headerName":"X-CSRF-TOKEN","token":"…"}`; session-bound, masked CSRF token |

The client retrieves CSRF before mutation and sends the returned header with same-origin cookies. Call `refreshCsrf()` after successful login and logout, then use `request<T>()` for subsequent API calls. Successful 204 responses return `undefined`. Do not retry mutations automatically. Protected resources are denied until authentication is implemented.

## Analytics event agreement for implementation

Alok publishes typed click/download events; Rana owns persistence/listeners. Each event carries a UUID `eventId`, UTC `occurredAt`, `ownerId` and `linkId`; downloads also carry `fileId`. No token, code, password or target URL belongs in an event. Publishers emit within their domain transaction, and analytics listeners process only after successful commit in their own transaction. Analytics persistence must enforce unique event IDs for idempotency; a failed listener must not turn a completed redirect/download into a reported failure. The analytics module must document and test recovery of failed event persistence before release. Clicks count accepted redirects; download counts represent atomically reserved authorized attempts, including failed streams. Sharing owns reservation enforcement; analytics never authorizes access. These are proposed integration contracts for peer review, not claims of implemented analytics.

## Verification and collaboration

Run `./mvnw -B verify` from `backend/` (after the frontend build) to execute full-context MockMvc tests against the PostgreSQL configured in the environment. There is no H2 fallback. Frontend checks are `npm ci`, `npm test` and `npm run build`. CI runs **Backend verification** with a fresh PostgreSQL 17 service and **Frontend verification** on PRs targeting develop/main and pushes to those branches. Required checks must be configured after the first successful workflow run, before merging. Integration tests count separately from service unit tests; foundation checks do not fulfill later service-test allocations.

Work uses scoped feature branches and PRs to `develop`, independent approval, resolved conversations, and merge commits. No direct protected-branch writes, automatic merging or deployment is part of this foundation. Containerization and gated main-only Render deployment are later work; no deployment is claimed.
