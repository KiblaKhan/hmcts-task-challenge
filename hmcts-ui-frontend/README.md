# HMCTS UI Frontend (Customised)

A frontend for the HMCTS task challenge, built on top of
[`hmcts-dev-test-frontend`](https://github.com/hmcts/hmcts-dev-test-frontend). It provides a
task list, detail pages, creation flow, and a responsive calendar view. It integrates with the
Spring Boot API (`hmcts-api-service/api` in the HMCTS Challenge repository) and keeps the
GOV.UK Design System look and feel.  Note: tested on Windows 11 only.

> **Highlights**
>
> - **Responsive** task list: table on desktop, compact cards on mobile.
> - **Calendar view**: equal-sized day boxes with task “dots”; centered overlay card for details.
> - **Sort & filter**: sort by `dueDate` or `status`; client‑side filter by `status`.
> - **Task actions**: Start, Complete, Delete.
> - **Windows-friendly dev**: shell script replaced by Node script to generate local HTTPS certs.
> - **Clean server code**: TypeScript, Nunjucks templating, auto‑loaded routes, tests with Jest/Supertest.

---

## Contents

- [HMCTS UI Frontend (Customised)](#hmcts-ui-frontend-customised)
  - [Contents](#contents)
  - [Architecture](#architecture)
    - [Data flow](#data-flow)
    - [Backend API](#backend-api)
  - [Getting Started](#getting-started)
    - [1) Install](#1-install)
    - [2) Run the backend API](#2-run-the-backend-api)
    - [3) Run the frontend (HTTPS)](#3-run-the-frontend-https)
  - [Configuration](#configuration)
  - [Available Scripts](#available-scripts)
  - [Testing](#testing)
    - [Unit test coverage expectations](#unit-test-coverage-expectations)
    - [Suggested unit tests](#suggested-unit-tests)
    - [Route/API integration tests](#routeapi-integration-tests)
    - [End‑to‑end (browser) tests](#endtoend-browser-tests)
    - [Accessibility checks](#accessibility-checks)
  - [Design \& Principles](#design--principles)
    - [SOLID in practice](#solid-in-practice)
    - [Patterns used](#patterns-used)
  - [Future improvements](#future-improvements)
  - [Troubleshooting](#troubleshooting)

---

## Architecture

```
src/main
├─ app.ts                    # Express app: Nunjucks, static, middleware, route auto-loader
├─ server.ts                 # HTTPS server bootstrapping
├─ modules/
│  └─ nunjucks/              # Nunjucks env + filters (e.g. fmtDate)
├─ services/
│  └─ tasksApi.ts            # Axios client for the Spring API + DTOs
├─ routes/
│  ├─ home.ts                # Welcome page + sample API call
│  └─ tasks.ts               # List / Calendar / Details / Create / Status actions
└─ views/
   ├─ layout.njk             # Base layout
   ├─ home.njk               # Welcome
   └─ tasks/
      ├─ list.njk            # Table (desktop) + compact cards (mobile), filters, toolbar
      ├─ form.njk            # Create Task
      ├─ details.njk         # Details + action buttons
      └─ calendar.njk        # Grid calendar + centered overlay; compact list on phones
```

### Data flow

- UI makes server‑side calls via `services/tasksApi.ts`.
- Routes prepare **view models** (e.g., flags `_ymd`, `_overdue`, `_dueToday`) and render Nunjucks templates.
- Client‑side JS is minimal and progressive (e.g. small script to open the centered overlay).

### Backend API

- `GET /tasks{?sort}` with `sort=dueDate|status`
- `GET /tasks/{id}`
- `POST /tasks`
- `PUT /tasks/{id}/status`
- `DELETE /tasks/{id}`

> Note: The API doesn’t support server‑side `status` filtering, so the frontend applies it client‑side.

---

## Getting Started

Prerequisites:

- Node.js ≥ 18
- Yarn 3 (Berry)
- Spring Boot API running locally (from the challenge repo)

### 1) Install

```bash
yarn install
```

### 2) Run the backend API

From the challenge repo `hmcts-api-service/api`:

```bash
./mvnw spring-boot:run
# API at http://localhost:8080
```

### 3) Run the frontend (HTTPS)

```bash
yarn start:dev
# opens https://localhost:3100
```

On first run, a Node script creates self‑signed certs in
`src/main/resources/localhost-ssl/` so the dev server runs over **HTTPS**.

---

## Configuration

Environment variables:

- `PORT` — HTTPS port (default: `3100`)
- `TASKS_API_URL` — API base URL (default: `http://localhost:8080`)
- `NODE_ENV` — `development` or `production`

Path aliasing (via `tsconfig-paths`) keeps imports neat in `src/main`.

---

## Available Scripts

```jsonc
yarn start         // production mode start (ts-node)
yarn start:dev     // dev with nodemon + HTTPS cert generation
yarn build         // webpack assets (JS/CSS) for dev
yarn build:prod    // webpack assets in production mode
yarn lint          // eslint + stylelint + prettier check
yarn test          // runs unit tests locally (alias to test:unit)
yarn test:unit     // jest
yarn test:routes   // (optional) separate jest config for route tests
```

---

## Testing

This project uses **Jest** for unit tests and **Supertest** for route/API tests.
Playwright/CodeceptJS are available for end‑to‑end scenarios.

### Unit test coverage expectations

Suggested thresholds (configure in `jest.config.js`):

```js
coverageThreshold: {
  global: { statements: 90, branches: 85, functions: 90, lines: 90 }
}
```

### Suggested unit tests

**1) `services/tasksApi.test.ts`**

- ✅ calls `GET /tasks` with the right query params (sort).
- ✅ maps API responses into `Task` correctly (including `dueAt` shapes).
- ✅ errors: network error → throws; 4xx/5xx → throws with message.
- ✅ `updateTaskStatus(id, 'IN_PROGRESS'|'DONE')` uses `PUT /tasks/{id}/status` with correct body.
- ✅ `createTask` validates required fields and passes ISO date when provided.

<details>
<summary>Example (nock)</summary>

```ts
import nock from 'nock';
import { listTasks, updateTaskStatus, createTask } from '../services/tasksApi';

const API = 'http://localhost:8080';

describe('tasksApi', () => {
  afterEach(() => nock.cleanAll());

  it('lists tasks with sorting', async () => {
    nock(API).get('/tasks').query({ sort: 'dueDate' })
      .reply(200, [{ id: '1', title: 'A', description: null, status: 'OPEN', dueAt: null }]);
    const tasks = await listTasks({ sort: 'dueDate' });
    expect(tasks).toHaveLength(1);
    expect(tasks[0].title).toBe('A');
  });

  it('updates status', async () => {
    nock(API).put('/tasks/1/status', { status: 'DONE' }).reply(204);
    await expect(updateTaskStatus('1','DONE')).resolves.toBeUndefined();
  });

  it('creates task', async () => {
    nock(API).post('/tasks', body => body.title === 'X').reply(201, { id: '9', title: 'X', status: 'OPEN' });
    const t = await createTask({ title: 'X' });
    expect(t.id).toBe('9');
  });
});
```

</details>

**2) `modules/nunjucks/filters.test.ts`**

- ✅ `fmtDate` renders ISO → “DD Mon YYYY” (or the project format), `null`/invalid → `—`.
- ✅ any custom filters/macros render safely on undefined values.

**3) `routes/tasks.test.ts` (unit-ish)**

- Mock `services/tasksApi` to control responses; `supertest` the Express app.
- ✅ `GET /tasks` renders 200 and includes titles; query `?status=OPEN` filters client‑side.
- ✅ `GET /tasks/calendar` renders 200; month labels behave; days from adjacent months are muted.
- ✅ `POST /tasks` validates title; on success redirects to `/tasks/:id`.
- ✅ `POST /tasks/:id/start|complete|delete` redirect and call services.

<details>
<summary>Example (supertest + jest.mock)</summary>

```ts
import request from 'supertest';
import { app } from '../app';
jest.mock('../services/tasksApi', () => ({
  listTasks: jest.fn().mockResolvedValue([
    { id:'1', title:'A', status:'OPEN', dueAt:null },
    { id:'2', title:'B', status:'DONE', dueAt:'2025-09-30T17:00:00Z' },
  ]),
  getTask: jest.fn().mockResolvedValue({ id:'1', title:'A', status:'OPEN', dueAt:null }),
  createTask: jest.fn().mockResolvedValue({ id:'9' }),
  updateTaskStatus: jest.fn().mockResolvedValue(undefined),
  deleteTask: jest.fn().mockResolvedValue(undefined),
}));
describe('routes/tasks', () => {
  it('lists tasks with client-side status filter', async () => {
    const res = await request(app).get('/tasks?status=OPEN');
    expect(res.status).toBe(200);
    expect(res.text).toMatch(/Tasks/);
    expect(res.text).toMatch(/A/);
    expect(res.text).not.toMatch(/B/);
  });
});
```

</details>

**4) View smoke tests (optional)**

- Render `list.njk`/`calendar.njk`/`details.njk` with minimal context using Nunjucks env to catch template regressions (e.g. missing variables).

### Route/API integration tests

**Goal:** exercise the full Express stack against a **stubbed HTTP API**.

- Use **nock** to stub the Spring endpoints while hitting `supertest(app)`.
- Happy paths: list, details, create, status updates, delete.
- Error paths: 500/timeout from API → error template is rendered; user‑friendly message present.

**Stretch:** spin the real Spring API on a random port in CI and point `TASKS_API_URL` to it (slower but closest to production).

### End‑to‑end (browser) tests

Use **Playwright**/**CodeceptJS** (already in dev deps, not tested). Scenarios:

- yarn test:functional - CodeceptJS + Playwright (browser E2E)
- Visit `/tasks`, filter by Status=OPEN, Sort=dueDate → list updates.
- Switch to **Calendar view**, click a dot → overlay opens; close via mouse, via Escape.
- Create task → redirected to details; click **Start**, then **Complete** → tags update.
- Mobile layout: emulate iPhone viewport → compact cards visible, toolbar “Apply + Calendar icon” arrangement correct.

Run (example):

```bash
NODE_TLS_REJECT_UNAUTHORIZED=0 yarn test:functional
```

### Accessibility checks

- Axe (`jest-axe`) for HTML rendered by `supertest` (server‑side render), or `@axe-core/playwright` for E2E.
- Focus management: overlay sets focus to Close, returns to dot on close.
- Keyboard only: dots are reachable; Enter/Space opens overlay; Esc closes.
- Colour contrast: GOV.UK colours are used for tags/buttons.

---

## Design & Principles

### SOLID in practice

- **S — Single Responsibility**: routes only orchestrate; `tasksApi` handles HTTP; Nunjucks handles presentation; filters format values.
- **O — Open/Closed**: extendable view logic via small helpers (e.g. flags like `_overdue`) without changing consumers.
- **L — Liskov Substitution**: route code depends on the `Task` interface; alternate implementations of `tasksApi` (e.g., mock) can be swapped.
- **I — Interface Segregation**: consumer shapes (`Task`, `UpdateStatusPayload`) are small and focused.
- **D — Dependency Inversion**: routes depend on `tasksApi` abstraction, not Axios directly; allows mocking and testing.

### Patterns used

- **MVC (lightweight)**: Models from API, Views in Nunjucks, Controllers in Express routes.
- **Adapter/Facade**: `tasksApi` adapts the Spring API to a typed interface.
- **ViewModel mapping**: server adds `_ymd`, `_overdue`, `_dueToday` for display logic.
- **Progressive enhancement**: calendar overlay uses small JS; page still renders without JS.

---

## Future improvements

- **Server‑side filter by status** once the API supports it.
- **Pagination UI** for long task lists; graceful handling when API enforces limits.
- **i18n** with Nunjucks/`i18next` for labels and date formats.
- **Form validation**: server + client with GOV.UK error summary components.
- **Security**: enable `helmet`, CSRF protection where forms mutate state, strict cookies.
- **Observability**: wire up Application Insights (already in deps), add route timing and error telemetry.
- **Accessibility**: reduce tooltip reliance for keyboard users (already hover‑only), provide visible labels on high‑contrast mode.
- **Performance**: cache API responses briefly on the server; HTTP keep‑alive and timeouts in Axios client.
- **CI/CD**: GitHub Actions to run lint/tests/build; attach HTML test reports and coverage; optional Docker image build.

---

## Troubleshooting

- **“Cannot GET /tasks”**: ensure the route loader is pointing at `src/main/routes` and files export `default(app)`.
- **ECONNREFUSED** when opening tasks: verify API is running at `TASKS_API_URL` (default `http://localhost:8080`).
- **Nunjucks “expected variable end”**: check template syntax (use `{% set x = (a | default([])) %}` rather than `or` when in doubt).
- **Windows shell errors**: dev startup uses a Node script (`bin/generate-ssl-options.cjs`) to avoid `bash`/`sh`.
- **HTTPS dev certs**: if certs go stale, delete `src/main/resources/localhost-ssl/` and restart `yarn start:dev`.
