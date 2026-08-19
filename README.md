# Smart Travel and Expense Hub — Web (Frontend + Backend)

The web administration console for Smart Travel & Expense Hub, serving three roles:
Admin, Finance Staff, and Manager. Both tiers live in this repository:

```
web-frontend/   React + Vite frontend
web-backend/    Spring Boot backend
gateway/        Nginx gateway config — the single public entry point (TLS termination)
docker-compose.yml   Full-stack orchestration (MySQL + backend + frontend + gateway)
.github/workflows/
  ci-frontend.yml   Frontend CI/CD (5 stages: build, secret scan, SAST, SCA, image scan, DAST)
  ci-backend.yml    Backend CI/CD (same shape)
```

> The web tier is not a standalone system. Trip requests and expense claims are
> created by employees on the **mobile team's backend**; this repository reads and
> writes that data over two separate channels (see "Integration with the mobile
> backend"). Without the mobile service running, the reimbursement, approval, and
> analytics modules have no business data to work with.

---

## 1. Architecture

```
                          ┌──────────────────┐
        external      ──▶ │     gateway      │  Nginx, sole public entry point
        traffic (HTTPS)   │  :80 → 301 :443  │  TLS 1.2/1.3 termination
                          └────────┬─────────┘
              /api/*   ┌───────────┼────────────┐  /mobile-uploads/*
                       │           │ everything │
                       ▼           ▼   else     ▼
              ┌────────────────┐ ┌──────────────────┐ ┌──────────────┐
              │  web-backend   │ │   web-frontend   │ │ mobile tier  │
              │ Spring Boot 4  │ │  React static    │ │ receipt      │
              │    :8081       │ │  assets (nginx)  │ │ images       │
              └───┬────────┬───┘ └──────────────────┘ └──────────────┘
                  │        │
     primary      │        │  mobile datasource + REST
     datasource   ▼        ▼
          ┌──────────┐   ┌───────────────────────────┐
          │  mysql   │   │  mobile team's MySQL / API │
          │ (web db) │   │  (host:3307 / host:8080)  │
          └──────────┘   └───────────────────────────┘
```

The frontend, backend, and MySQL expose no ports to the outside world. Only
`gateway` listens on 80 and 443, and port 80 permanently redirects to 443. This
satisfies backlog Item 29 ("Nginx as a unified API gateway") and explains why both
CI pipelines build their own images while only the gateway layer needs an open port
on the server.

### Technology stack

| Layer | Choice |
|---|---|
| Backend | Spring Boot 4.0.7 / Java 21 / Spring Security + JWT (jjwt 0.12.6) / Spring Data JPA / Apache POI 5.3 / Lombok |
| Frontend | React 18 / Vite 5 / react-router-dom 7 / axios / Chart.js 4 + react-chartjs-2 |
| Database | MySQL 8.4 (tests run against H2 in-memory in MySQL compatibility mode) |
| Gateway | Nginx 1.27-alpine, TLS 1.2/1.3 |
| Orchestration & deployment | Docker Compose, DigitalOcean Ubuntu Droplet, build-on-server (no image registry) |
| Quality & security | GitHub Actions + TruffleHog + CodeQL + Snyk + Trivy + OWASP ZAP + JaCoCo |

### Two JPA datasources

The backend mounts two datasources simultaneously. This is the single most important
architectural detail to be aware of when working on this codebase.

| Datasource | Config class | Owns |
|---|---|---|
| **primary** (web-owned database) | `PrimaryJpaConfig` | `BudgetConfig`, `DepartmentalBudget`, `ExpenseApprovalWorkflow`, `User`, and the two audit log tables |
| **mobile** (the mobile team's database) | `MobileDataSourceConfig` | Only the `Approval` entity (trip approval table), plus a `JdbcTemplate` used to update the mobile `trips.status` column directly |

Connecting straight to the mobile database rather than keeping a local copy was a
deliberate team decision: trip approval state must be **one shared source of truth**
so the web console and the mobile app always agree. A replicated copy drifted.

Because two datasources are in play, Boot 4's auto-configuration cannot reliably
resolve the `EntityManagerFactory` bean name, so the primary `DataSource`,
`EntityManagerFactory`, and `TransactionManager` are all declared explicitly in
`PrimaryJpaConfig` and annotated `@Primary`. Repository package scanning is scoped
separately by `PrimaryJpaRepositoriesConfig` and `MobileJpaRepositoriesConfig` so the
two never overlap.

### Integration with the mobile backend

Integration runs over **two channels** with different responsibilities:

1. **Shared SQL** (the `mobile` datasource) — used only for trip approvals.
   `ManagerService.decide()` writes the `approvals` table and flips `trips.status` in
   the same database, so the mobile app's trip list reflects the decision immediately.
2. **REST calls** (`integration/mobile/MobileExpenseClient`) — expense claims, user
   profiles, and receipts. Mobile endpoints consumed: `GET /api/admin/expenses`,
   `GET /api/admin/trips`, `GET /api/users/{id}`, and
   `POST /api/admin/expenses/{approve|reject|request-info}`.

Both systems **share the same `JWT_SECRET`**, so the web tier can mint tokens the
mobile tier trusts. `MobileAuthTokenProvider` issues two distinct kinds of token, and
the difference matters:

- `currentUserTokenForMobile()` — carries the identity and role of the logged-in web
  user (web roles map to mobile roles: `FINANCE_STAFF → FINANCE`, others unchanged).
  Used for actions taken on a user's behalf.
- `serviceAccountTokenForMobile()` — a dedicated service account,
  `web-integration-service`, with the MANAGER role on the mobile side. This is
  **required**, not a convenience: the mobile `getUserById()` endpoint resolves the JWT
  subject back to a real mobile user record, and web users have no mobile account. The
  `@Scheduled(fixedRate = 60000)` auto-sync job in `ManagerService` also runs with no
  logged-in user context at all.

Enum values deliberately mirror the mobile side so neither team has to translate names:
`ReimbursementStatus` (SUBMITTED / APPROVED / REJECTED / NEEDS_INFO) and
`ReimbursementCategory` (FLIGHT / HOTEL / MEAL / TRANSPORT / OTHER).
**The mobile tier is always the authoritative source for expense data** — the web tier
neither persists nor caches it.

### Same-origin receipt proxy

Mobile returns `receiptUrl` values shaped like
`/uploads/receipts/2026-08-13/xxx.png`. `FinanceServiceImpl.buildFullReceiptUrl()`
rewrites these to `/mobile-uploads/...`, and the gateway's `location /mobile-uploads/`
block reverse-proxies them to `${MOBILE_UPSTREAM}/uploads/`. The browser therefore
stays same-origin throughout, and the mobile tier's internal address and port are never
exposed to the client.

### Role-based access control

There are exactly three roles: `ADMIN`, `FINANCE_STAFF`, `MANAGER`
(`entity/Role.java`). `SecurityConfig` authorizes by path prefix, sessions are
STATELESS, and `JwtAuthenticationFilter` parses the token ahead of
`UsernamePasswordAuthenticationFilter`.

| Path prefix | Allowed roles |
|---|---|
| `/api/auth/**`, `/api/health` | public |
| `/api/admin/**` | ADMIN |
| `/api/finance/**` | ADMIN, FINANCE_STAFF, MANAGER |
| `/api/manager/**` | ADMIN, MANAGER |
| all other `/api/**` | any authenticated user |

The frontend mirrors this with route-level guards in `ProtectedRoute`, but **the server
is the only authority on permissions**.

### Data encryption (Item 26)

- **In transit** — TLS 1.2/1.3 at the gateway; the backend's JDBC URL enables
  `useSSL=true&requireSSL=true`.
- **At rest** — `EncryptionService` uses AES-256-GCM. Every encryption generates a
  random 12-byte IV, prepends it to the ciphertext, and Base64-encodes the result, so
  identical plaintexts always produce different ciphertexts. Encryption and decryption
  are transparent at the field level through a JPA `AttributeConverter`
  (`EncryptedStringConverter`); encrypted columns are declared as `TEXT`.
- **Queryability** — an encrypted `User.email` cannot be matched by equality, so an
  additional `email_hash` column (SHA-256 hex, unique index) is persisted. Login and
  identity lookups go through that column.
- **Migrating existing data** — `DataEncryptionMigration` (prod profile only) re-saves
  existing plaintext records at startup to trigger encryption. The converter falls back
  to returning the raw value when decryption fails, so nothing breaks mid-migration. The
  job is idempotent, keyed on whether any user already has an `emailHash`.

---

## 2. Running locally

```bash
cp .env.example .env   # edit as needed — see the table below
docker compose up --build
```

- Open `https://localhost` → gateway → frontend (a self-signed certificate will
  trigger a browser warning; this is expected)
- `https://localhost/api/health` → gateway → backend health check

Generate a self-signed certificate with:

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout gateway/certs/privkey.pem -out gateway/certs/fullchain.pem \
  -subj "/CN=localhost"
```

### Environment variables

| Variable | Notes |
|---|---|
| `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` / `DB_ROOT_PASSWORD` | Web-owned MySQL |
| `JWT_SECRET` | **Required**, 32+ characters. Must match the mobile tier's value or cross-system calls are rejected |
| `ENCRYPTION_SECRET` | **Required**, Base64-encoded 32-byte key (`openssl rand -base64 32`). Losing it makes already-encrypted data unrecoverable |
| `MOBILE_API_BASE_URL` | **Required**, address of the mobile REST service |
| `MOBILE_UPSTREAM` | **Required**, `host:port` (no scheme) the gateway proxies receipt images to |
| `MOBILE_DB_URL` / `MOBILE_DB_USERNAME` / `MOBILE_DB_PASSWORD` | Mobile MySQL (defaults to `host.docker.internal:3307`) |
| `SEED_DEMO_USERS` | Demo data toggle |

The `web-backend` and `gateway` services use `${VAR:?...}` syntax for required values,
so a missing variable fails the container at startup rather than letting it run with a
bad configuration.

Demo credentials live in `TEST_ACCOUNTS.md` (`UserDataSeeder` only activates under the
`dev` profile, and that file is the source of truth for the values). Five consecutive
failed logins lock an account; an admin can release it via
`POST /api/admin/users/{id}/unlock`.

---

## 3. Two independent CI/CD pipelines

One pipeline each for frontend and backend, triggered by changes to their own directory
or to `docker-compose.yml` (PRs targeting `main`/`develop`, pushes to `main`). Both
`deploy` jobs share the `concurrency: expense-hub-production` group so they run
serially, never racing each other on `git pull` and `docker compose up` on the same
server.

### The five stages

| Stage | Job | Frontend | Backend |
|---|---|---|---|
| 1 Build & test | `build-and-test` | npm ci → ESLint → Prettier check → Vitest + coverage → vite build | `mvn test` (H2, `test` profile) → `mvn package` → JaCoCo report |
| 2a Secret scanning | `secret-scan` | TruffleHog with `fetch-depth: 0` — scans **all history**, not just the diff; `fail: true` | same |
| 2b SAST | `codeql` | CodeQL (javascript-typescript) | CodeQL (java-kotlin; needs `mvn compile` first to analyze bytecode) |
| 2c Dependency CVEs | `dependency-scan` | Snyk (npm, `--severity-threshold=high`) | Snyk CLI (`pom.xml`, same threshold) |
| 3 Image build & scan | `docker-build` | build image, then Trivy scan; CRITICAL findings set `exit-code: 1` | same, but scoped to `vuln-type: os` |
| 5 DAST | `dast` | run the container, then OWASP ZAP baseline scan against the live static site (security headers, cookie flags, information disclosure) | run the container plus a throwaway MySQL; ZAP probes `/api` as a black box |
| 4 Deploy | `deploy` | SSH → `git pull --ff-only` → `docker compose up -d --build web-frontend` | same, with `web-backend` |

(Stage numbers follow the comments in the workflow files. The actual dependency order
is `deploy` **needs** `dast`, meaning DAST runs *before* deployment — this was the point
of commit `e40a53f`: make every security check a release gate.)

### Why the scanners don't overlap

Responsibilities are split deliberately, to avoid both duplicate noise and blind spots:

- **Snyk** owns application dependency CVEs — it reads `pom.xml` and `package.json`,
  covering Java libraries and npm packages.
- **Trivy** scans only the container image's **OS packages** (the backend sets
  `vuln-type: os` explicitly) and never looks at JARs.
- **CodeQL** analyzes **first-party code** for injection, broken access control, unsafe
  API usage, and similar issues. It does not examine dependencies.
- **TruffleHog** looks for **credentials in commit history**, an entirely different
  dimension from the other three.

Trivy results are uploaded as SARIF to the GitHub Security tab, and the vulnerability
database is cached so it isn't re-downloaded on every run.

### Required status checks

A failing step turns the corresponding job red, but **a red cross does not block a merge
by itself**. To make it actually gate a PR, the following must be configured once by
hand under Settings → Branches → Branch protection rules, for both pipelines:
`build-and-test`, `secret-scan`, `codeql`, and `dependency-scan` set as required status
checks. The workflow files cannot do this themselves.

### Deployment secrets

The project builds on the server and pushes nothing to an image registry, so no
`REGISTRY_*` variables are needed. Only these are required: `DEPLOY_HOST`,
`DEPLOY_USER`, `DEPLOY_SSH_KEY`, and `SNYK_TOKEN`.

The deployment directory on the server must be a working copy of this repository's
`main` branch with permission to `git pull`. Production environment variables belong in
a `.env` file on the server itself.

---

## 4. Web Backend

### Directory layout

```
src/main/java/com/expensehub/webbackend/
  config/               Security, CORS, dual datasources, RestClient, scheduling,
                        data seeders, encryption migration
  security/             JwtUtil, JwtAuthenticationFilter, EncryptionService,
                        EncryptedStringConverter, HashUtil
  controller/           REST endpoints (8 controllers)
  service/              Business logic; implementations under impl/
  entity/               Primary-datasource JPA entities and enums
  repository/           Primary-datasource Spring Data JPA repositories
  mobile/entity/        Mobile-datasource entity (Approval only)
  mobile/repository/    Mobile-datasource repository
  integration/mobile/   Mobile REST client, DTOs, token provider, exceptions
  dto/                  Request/response DTOs (mostly records)
  exception/            Custom exceptions + GlobalExceptionHandler (structured JSON errors)
```

### Main endpoints

| Module | Endpoints | Backlog |
|---|---|---|
| Authentication | `POST /api/auth/login` | 1 |
| Health check | `GET /api/health` | — |
| Account management | `GET/POST /api/admin/users`, `PUT /{id}`, `PATCH /{id}/status`, `POST /{id}/unlock` | 2 |
| Budget configuration | `GET/PUT /api/finance/budgets`, `GET /budgets/{id}/audit` | 16 |
| Reimbursement review | `GET /api/finance/reimbursements` (paged, multi-filter), `GET /{id}`, `PATCH /{id}/review`, `GET /{id}/audit` | 17, 19 |
| Excel export | `GET /api/finance/reimbursements/export` | 18 |
| Trip approvals | `GET /api/manager/approvals/{pending,history}`, `POST /{id}/{approve,reject}` | 20 |
| Over-budget expense approvals | `GET /api/manager/expense-approvals/{pending,history}`, `POST /{id}/{approve,reject}`, `GET /{id}/decision` | 20 |
| Trip budget caps | `GET/POST /api/manager/budgets`, `PUT /{id}` | 20 |
| Analytics | `GET /api/analytics/{department-expenses,travel-frequency,budget-alerts,expense-categories,monthly-trend,approval-outcomes}` | 23–25 |

> ⚠️ **There are two "budget" concepts and two "approval" concepts. The names are
> similar; the semantics are not:**
> - `/api/finance/budgets` (`BudgetConfig`, Item 16) — reimbursement spending budget,
>   quarterly or annual, drives the `OVER_BUDGET` policy flag.
> - `/api/manager/budgets` (`DepartmentalBudget`, Item 20) — annual departmental cap
>   used for trip pre-approval.
> - `/api/manager/approvals` — approval of employees' **trip requests** (data lives in
>   the mobile `approvals` table).
> - `/api/manager/expense-approvals` — manager sign-off required *before* finance review
>   when a **claim exceeds budget** (data lives in the web `expense_approval_workflow`
>   table).

### Reimbursement review flow (Items 17 / 19 / 20)

```
Mobile /api/admin/expenses
        │  MobileExpenseClient.listAllExpenses()
        ▼
FinanceServiceImpl.fetchResolvedExpenses()      ← resolve submitter's department (cached)
        │
        ├─▶ ExpenseApprovalWorkflowService       ← decide if manager sign-off is needed
        │        needsManagerApproval = OVER_BUDGET flag present
        │        readyForFinance = !needsManagerApproval || managerApproved
        │
        ├─▶ ReimbursementPolicyEngine.evaluate() ← produce policy flags
        │        MISSING_RECEIPT / OVER_PER_DIEM / OVER_BUDGET
        │
        └─▶ only readyForFinance records reach the finance list and the export
                 │
                 ▼  PATCH /review
        MobileExpenseClient.{approve,reject,requestInfo}
                 │
                 ▼
        ReimbursementAuditLog (web-side trail)
```

Per-diem limits (`ReimbursementPolicyEngine`): MEAL 75, HOTEL 300, TRANSPORT 100,
FLIGHT 1500, OTHER 150. Budget periods are resolved uniformly through
`BudgetPeriodResolver` (quarter labels like `2026-Q3`, annual labels like `2026`), with
quarterly taking priority over annual. When neither is configured, that means "no limit
set" rather than "a limit of zero" — a rule owned solely by `BudgetLookupService` and
shared by the approval and analytics modules so the two never drift into separate
implementations.

### Audit logging (Item 27)

Two separate tables: `BudgetAuditLog` records budget amount changes (previous value,
new value, actor, timestamp), and `ReimbursementAuditLog` records every action taken on
a claim (`REVIEW_*`, `MANAGER_APPROVE`, `MANAGER_REJECT`). The actor is always derived
from the JWT identity and never from a client-supplied id — as the comment on
`ManagerController.currentUserId()` explains, trusting a `managerId` in the request body
would let any authenticated caller attribute a decision to someone else.

### Local development

```bash
# Requires a local MySQL, or just use the mysql service from the root docker-compose
export DB_HOST=localhost DB_PORT=3306 DB_NAME=expense_hub DB_USERNAME=root DB_PASSWORD=xxx
export JWT_SECRET=any-random-string-at-least-32-characters-long
export ENCRYPTION_SECRET=$(openssl rand -base64 32)
export MOBILE_API_BASE_URL=http://localhost:8080
mvn spring-boot:run
```

Run the same checks CI runs before you push:

```bash
mvn test       # unit/integration tests (H2; both datasources point at in-memory DBs)
mvn package    # package, confirming there are no compilation errors
```

Installing **Extension Pack for Java** and **Spring Boot Extension Pack** in VS Code
gives you debugging, breakpoints, and bean dependency graphs comparable to IntelliJ.

### Docker

Multi-stage build: `maven:3.9-eclipse-temurin-21` compiles, `eclipse-temurin:21-jre-alpine`
runs. The runtime stage applies `apk upgrade` for OS patches, creates a non-root `spring`
user to run the process, defines a `/api/health` HEALTHCHECK, and sets
`MaxRAMPercentage=50` so the JVM respects the container memory limit.

---

## 5. Web Frontend

### Directory layout

```
src/
  api/          axios client (JWT injection + 401 interception) and per-module APIs
  components/   BasicLayout (role-aware navigation), ProtectedRoute
  pages/        Login / Dashboard / per-role feature pages / 404
  charts/       Chart.js components and chartSetup
  mocks/        Mock data for local development
  utils/        auth (token and role storage), insights
  styles/       Global styles and theme
tests/          Vitest unit tests
```

### Routes

| Route | Page | Allowed roles |
|---|---|---|
| `/login` | Login | public |
| `/` | Dashboard | any authenticated user |
| `/admin/accounts` | Admin account management | ADMIN |
| `/admin/accounts/create` | Account creation and role assignment | ADMIN |
| `/finance/reimbursements` | Reimbursement review and export | ADMIN, FINANCE_STAFF |
| `/manager/approvals` | Trip request approvals | ADMIN, MANAGER |
| `/manager/expense-approvals` | Over-budget expense approvals | ADMIN, MANAGER |
| `/analytics` | Analytics and visualization | any authenticated user |
| `*` | 404 | — |

The axios instance in `api/client.js` injects the `Authorization` header from
`localStorage` on every request, and on a 401 it clears local credentials and redirects
to `/login`.

### Local development

```bash
npm install
npm run dev        # dev server; /api proxies to http://localhost:8081 by default
```

Run the same checks CI runs before you push, so PRs don't get blocked:

```bash
npm run lint            # ESLint (--max-warnings 0)
npm run format:check    # Prettier
npm run test:coverage   # Vitest + coverage
npm run build           # production build
```

Opening the project in VS Code prompts you to install the recommended extensions from
`.vscode/extensions.json` (ESLint, Prettier, Docker, Vitest). Format-on-save and
auto-fix are preconfigured, so you see what CI would flag while you write rather than
after the pipeline runs.

### Docker

Multi-stage build: `node:20-alpine` compiles the static assets, and only the output is
packaged into `nginx:1.27-alpine` — small image, small attack surface. `nginx.conf`
already handles the React Router client-side fallback.

```bash
docker build -t expense-hub-web .
docker run -p 8081:80 expense-hub-web
```

---

## 6. Known issues and follow-up work

- **Item 3 (password reset by email)** has not been started.
- **Over-budget detection has two inconsistent implementations.**
  `FinanceServiceImpl.computeFlags()` sums the department's spend across the whole
  budget period, while `ExpenseApprovalWorkflowService.calculateFlags()` currently
  compares a single claim against the budget (its own comments mark this as
  simplified). These need to converge.
- **The reimbursement list filters, sorts, and pages in memory** (`PageImpl` +
  `subList`). This follows from the authoritative data living behind a mobile REST API
  rather than in a local table, but it will need server-side paging or a local
  projection table as volume grows.
- `ExpenseApprovalWorkflow.department` has field-level encryption applied while also
  being used for equality queries and indexed. AES-GCM is non-deterministic, so this
  needs review — `BudgetConfig` already dropped encryption on that field in commit
  `9e19033`.
- The self-signed certificates under `gateway/certs/` **should not be committed to
  version control**. Production should use a real certificate, and the private key
  should be removed from the repository history.
- CORS is currently `allowedOriginPatterns("*")` with `allowCredentials(true)`. This
  should be narrowed to specific origins before production use.
