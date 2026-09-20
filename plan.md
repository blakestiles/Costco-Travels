# Costco Travel Smart Rebook — Build Plan

**A resilient self-service reservation change prototype**

> Independent engineering prototype created for interview discussion. Not affiliated with Costco Wholesale or Costco Travel.

This is the authoritative build spec: the original plan, corrected for the verified local environment,
and re-scoped around how it will actually be used — a 5–10 minute demo followed by ~20 minutes of
technical questions in a Software Engineer II interview.

---

## 0. Scoping principle (read this before anything else)

**Mock aggressively outward. Never mock inward.**

| Layer | Treatment | Why |
|---|---|---|
| External suppliers, ServiceNow, payments, auth | **Mocked — unapologetically** | This is correct architecture, not a shortcut. "I don't have Costco's supplier contracts, so I built them as real HTTP services behind interfaces" is a strong answer. It is documented in README, never apologized for. |
| Orchestration, compensation, persistence, idempotency, event timeline | **Real, no compromise** | These are the only parts an interviewer will interrogate. A hardcoded timeline or a JS-simulated failure flips their read from *engineer* to *person who made a mockup*. |
| Non-demo UI surface | **Styled but inert** | Screen time is not the product. |

Nobody is hired off the demo; they are hired off the conversation the demo makes possible.
Every item below earns its place by **surviving a follow-up question**, not by filling screen time.

**The demo is one scenario, not four.** Car timeout → compensation → original reservation still
confirmed → event timeline read from SQL Server. Ninety seconds, and it is the only part a Costco
engineer will find genuinely interesting. The other scenarios are implemented and tested; they are
backup material, not the script.

**UI bar: "immediately familiar," not "pixel-perfect."** Pixel-matching buys ten seconds of *oh, nice*
and zero technical credibility, costs a large share of the budget, and drifts toward impersonation —
shown to the company being impersonated. Same layout skeleton, same restrained red/blue, same nav
vocabulary, text wordmark. Recognizable, clearly a prototype.

**Budget: ~$25–35 on Sonnet, ~3 hours** for everything in this document.

---

## 1. Environment facts (verified 2026-09-18)

| Item | Status | Consequence |
|---|---|---|
| Working dir | `C:\Users\saina\OneDrive\Desktop\Costco Travels` — empty, not a git repo | `git init` required |
| Java | 23.0.1 | Compile with `--release 21`; Spring Boot 3.4.x |
| Maven | **not installed**, no `~/.m2` | Bootstrap Maven Wrapper; first build downloads everything (10–20 min) |
| Docker | Desktop 29.2.0 installed, **daemon not running** | Must be started before SQL Server; image pull ~1.5 GB |
| Git | 2.53.0 | Available |
| Node | 24.13.0 | Unused — no JS build step, vanilla JS only |

---

## 2. Technical corrections to the original plan

Retained from the first revision. The first four would have broken the build outright.

1. **JSP forces WAR packaging.** Spring Boot cannot serve JSPs from an executable fat JAR.
   `booking-service` is packaged as `war` (executable — `java -jar` and `spring-boot:run` both work).
   Supplier services stay `jar`. Without this every UI page 404s.
2. **`PERCENTILE_CONT` is not a T-SQL aggregate** — it is window-only:
   `PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY latency_ms) OVER (PARTITION BY supplier)`, collapsed
   via a CTE. The original wording produces a proc that does not compile.
3. **Maven Wrapper bootstrap** — no Maven on PATH. Generate once, commit `mvnw`/`mvnw.cmd`.
   All docs use `./mvnw`, never `mvn`.
4. **Flyway needs `flyway-sqlserver`** (split out in Flyway 10+) and `GO` batch separators around the
   stored-procedure migration.
5. **No H2 substitution.** Integration tests run against the compose SQL Server under a `test` profile
   (`smartrebook_test` database), tagged `@Tag("integration")`, bound to failsafe so `./mvnw test`
   stays fast. Testcontainers is opt-in (`-Pit-testcontainers`), not default — the image is ~1.5 GB.
6. **No Costco logo asset.** Text wordmark only. Avoids any trademark question in a demo shown to Costco.
7. **Demo scenario propagation** via an `X-Demo-Scenario` header injected by a Feign `RequestInterceptor`,
   rather than fanning `/api/demo/scenario` out to three services.
8. **Resilience4j without TimeLimiter.** Hard timeouts come from Apache HttpClient 5 connect/read
   timeouts on the Feign client — synchronous and easy to explain. TimeLimiter would force the whole
   orchestration async for no demo value.
9. **One terminal state.** Change-request status is `FAILED`; reconciliation lives in a separate
   `reconciliation_status` column (`NOT_REQUIRED | REQUIRED | IN_PROGRESS | RESOLVED`).
10. **`shared-contracts` module included but minimal** — supplier DTO records only. The build-time
    coupling tradeoff is documented in README rather than hidden.
11. **Ports:** booking 8080, hotel 8081, car 8082, SQL Server 1433.
12. **Web research is best-effort.** Attempt the public Costco Travel site for visual reference; if
    fetches are blocked, proceed from the written direction in §8 and say so in the final report.
    Never log in, never scrape member data, never copy source.

---

## 3. Product framing (non-negotiable in all copy)

The public Costco Travel flow for some hotel/car changes asks the member to confirm a replacement and
then cancel the original. That is a sound, conservative design.

> Could part of that two-step member experience be safely orchestrated into a single self-service
> change workflow, while protecting the existing reservation when the replacement process fails?

**Never** write "Costco's system is inefficient" or "I fixed Costco Travel." The tone is curiosity
about a distributed-systems problem.

### Core invariant

> A reservation-change operation must not knowingly leave the member without their original valid
> reservation because a replacement transaction partially failed.

The replacement is secured **before** the original booking's final state changes.

---

## 4. Architecture

```
Browser (JSP + vanilla JS + fetch)
        |
        v
booking-service  :8080   (WAR, JSP views, REST API, orchestration, JPA + JDBC)
        |                         |
        | Feign/HTTP              +--> SQL Server :1433 (Flyway, JPA, T-SQL proc)
        |
        +--> hotel-supplier-service :8081  (in-memory reservations)
        +--> car-supplier-service   :8082  (in-memory reservations)
```

```
costco-travel-smart-rebook/
  pom.xml                     (pom packaging, Spring Boot BOM)
  shared-contracts/           (jar — supplier DTO records)
  booking-service/            (war — JSP UI, REST, orchestration, persistence)
  hotel-supplier-service/     (jar)
  car-supplier-service/       (jar)
  docker-compose.yml · Jenkinsfile · .env.example
  scripts/ · postman/ · docs/
```

Packages under `com.smartrebook.booking`: `controller` · `api` · `service` · `orchestration` ·
`repository` · `repository.jdbc` · `domain` · `dto` · `client` · `config` · `exception` · `demo` · `ops`

Rules: constructor injection only; records for DTOs; no single-impl interfaces **except** genuine
external boundaries (`HotelSupplierClient`, `CarSupplierClient`, `IncidentClient`); no method over
~40 lines; entities never leave the service layer.

---

## 5. Data layer — real

SQL Server 2022 in Docker. Flyway migrations:

- `V1__create_schema.sql` · `V2__create_indexes.sql` ·
  `V3__supplier_reliability_procedure.sql` · `V4__seed_demo_data.sql`

Tables: `members` · `bookings` · `booking_items` · `change_requests` · `supplier_reservations` ·
`booking_events` · `idempotency_records` · `incidents`. Columns per the original brief, plus
`change_requests.reconciliation_status` and `bookings.version` for JPA `@Version`.

Indexes, each carrying a comment justifying it — no decorative indexes:
- `booking_events (supplier, status, created_at)` — drives the reliability report
- `booking_events (correlation_id)` — transaction timeline lookup
- `bookings (confirmation_number)` unique — primary member lookup
- `idempotency_records (idempotency_key)` unique — the correctness guarantee itself
- `change_requests (booking_id, status)` — in-progress-change check

**Hibernate/JPA** for writes: entities, repositories, relationships, `@Version` optimistic locking,
DTO projection at the service boundary, explicit `@EntityGraph`/`join fetch` on booking→items and
change-request→events to avoid N+1. Decision documented.

**JDBC** for reads: `SupplierReliabilityJdbcRepository` calls `sp_supplier_reliability_report` via
`JdbcTemplate`. This is the concrete answer to "when would you use JDBC instead of Hibernate?"

**Transaction boundaries.** Local `@Transactional` units for local state only; external effects handled
by orchestration + compensation. README states plainly: *a relational transaction protects local state
but cannot atomically roll back independent external supplier systems.*

### Seeded data

Member: Sainath Gandhe · Executive Member · `DEMO-EXEC-001` (obviously synthetic).
Booking `CT-DEMO-78291`, `CONFIRMED`: Maui, Hawaii · Mar 14–19, 2027 · Wailea Beach Resort,
Ocean View King · Standard SUV · 2 adults · Hotel $1,780 + Car $360 + Taxes $200 = **$2,340** ·
$200 Digital Costco Shop Card.

Proposed (Mar 15–20): Hotel $1,820 + Car $370 + Taxes $220 = **$2,410** → **+$70**.
Replacement confirmation: `CT-DEMO-89412`.

---

## 6. Orchestration — the part that must be excellent

States: `CONFIRMED · CHANGE_PENDING · REPLACEMENT_PENDING · REPLACEMENT_CONFIRMED ·
CANCELLATION_PENDING · CANCELLED · FAILED · COMPENSATION_PENDING · RECONCILIATION_REQUIRED`.
No booleans like `isBooked`.

`RebookOrchestrator`, small testable methods: `createChangeRequest() · validateOriginal() ·
reserveHotel() · reserveCar() · compensateHotel() · compensateCar() · completeReplacement() ·
preserveOriginal() · markForReconciliation()`

Described as **"application-level orchestration with compensating actions inspired by the Saga pattern"** —
not "a Saga implementation."

**Happy path:** validate original CONFIRMED → idempotency check → correlation ID → create change request
→ reserve hotel → persist confirmation → reserve car → persist → REPLACEMENT_CONFIRMED → original
CANCELLATION_PENDING → cancel old supplier reservations → original CANCELLED → new booking
`CT-DEMO-89412` CONFIRMED → change request COMPLETED → events persisted.

**★ Car timeout — THE DEMO.** Hotel reserved; car POST exceeds the 3s read timeout **without** the
supplier creating a reservation. Compensate: cancel the replacement hotel reservation. Original preserved.
Events: `CHANGE_REQUESTED · HOTEL_RESERVATION_CONFIRMED · CAR_RESERVATION_TIMEOUT ·
COMPENSATION_STARTED · HOTEL_REPLACEMENT_CANCELLED · ORIGINAL_BOOKING_PRESERVED · CHANGE_FAILED`.

**Hotel failure** — availability succeeds, reservation POST returns 503, nothing to compensate.
*Implemented + tested; not in the demo script.*

**Ambiguous car result** — supplier **does** create the reservation, then delays past the client timeout.
booking-service cannot know whether it exists. **Do not retry. Do not assume failure.** →
`RECONCILIATION_REQUIRED`, original preserved. Ops **Reconcile** calls
`GET /api/cars/reservations/by-client-reference/{ref}`, finds the orphan, cancels orphan car + hotel,
sets `reconciliation_status = RESOLVED`. *Implemented + tested; kept as backup demo material because
**timeout ≠ confirmed failure** is the strongest Q&A talking point in the project.*

**Idempotency.** One `Idempotency-Key` per confirmation action. Backend stores key + SHA-256 request
hash + status + response. Same key + same hash → replay, zero supplier calls. Same key + different hash
→ `409`. *Implemented + tested; not demoed.*

**Concurrency.** `@Version` on `Booking` + in-progress-change guard → second concurrent change gets
`409 "Another change is already being processed for this reservation."`

**Retries.** Availability `GET` → bounded retry. Reservation `POST` → **no blind retry**.
Circuit breaker per supplier. Rationale in README.

**Correlation IDs.** Server-generated `CHG-7E9AC73C`, returned as `X-Correlation-ID`, propagated to
suppliers, stamped on every event, log line, and ops row.

---

## 7. Observability — real but lean

JSON structured logging via `logstash-logback-encoder`, MDC fields: `timestamp · level · service ·
correlationId · bookingId · changeRequestId · supplier · event · status · latencyMs · errorCode`.
Console + `logs/*.json`.

Optional Splunk HEC appender active **only** when `SPLUNK_HEC_URL` + `SPLUNK_HEC_TOKEN` are set.
No paid account needed. README does not claim a real Splunk deployment.

Actuator `/actuator/health` on all three services. Compose healthcheck on SQL Server.

---

## 8. Frontend — familiar, not replicated

Spring MVC + JSP (JSTL), embedded Tomcat, WAR. REST endpoints handle async work; `fetch()` drives
availability, replacement, reconcile, and demo controls. No React, no build step, no Tailwind.

### Built properly (the demo path)
`home.jsp` · `bookings.jsp` · `booking-details.jsp` · `change-booking.jsp` · `change-confirmation.jsp`
Fragments: `header.jsp` · `travel-nav.jsp` · `footer.jsp` · `booking-summary.jsp` ·
`price-comparison.jsp` · `demo-controls.jsp`

### Built thin
`operations.jsp` + `transaction-details.jsp` — **one ops page**: the transaction list, the timeline,
and the JDBC reliability table. **Metric cards cut.** Every non-demo travel link routes to a friendly
"Not included in this prototype" notice — never a dead anchor, never a broken page.

### Visual direction
Corporate travel portal, not a SaaS dashboard. White backgrounds, restrained red/blue accents,
horizontal nav (Deals · Destinations · Build Your Own Trip · Cruises · Rental Cars · Theme Parks &
Specialty), search tabs (Packages / Hotels / Cruises / Rental Cars), small radii, dense readable tables,
strong contrast. Explicitly avoid glassmorphism, large gradients, neon, oversized rounded cards,
floating sidebars, generic dashboard aesthetics.

**Target is recognizable, not identical.** One styling pass, checked at 1440px. No pixel-chasing loop.

### Flow
`/` → search module + "Maui Member Value" promo → **View Upcoming Trip** (one click to the demo) →
`/account/bookings` → **View Booking** → detail page → **Change Trip** → dates default to Mar 15–20 →
**Check Availability** (real supplier HTTP) → comparison with the +$70 delta →
**Replace Reservation** → confirm dialog:
*"Your existing reservation will remain active until the replacement is successfully secured."*

### Member-facing failure copy
"We couldn't complete your requested change." / "Your existing reservation is still confirmed." /
"Nothing has changed on your current trip." — with **Try Again** and **Return to Booking**.
No stack traces, no "saga", no "timeout", no HTTP codes in member UI.

### Accessibility
Semantic HTML, `<label for>` on every field, keyboard-operable tabs/dialogs, visible focus,
sufficient contrast, ARIA only where native semantics fall short.

---

## 9. Demo mode

Unobtrusive **Demo Controls** panel (demo profile only): Normal · Car Timeout · Hotel Failure ·
Ambiguous Car Response · Reset Demo. Note: *"Demo controls simulate external supplier behavior."*

- `POST /api/demo/scenario` — `NORMAL | HOTEL_FAILURE | CAR_TIMEOUT | CAR_AMBIGUOUS`, propagated by
  header; supplier behavior genuinely changes. Not a frontend illusion.
- `POST /api/demo/reset` — confirm "Reset all demo data?" → clears events, change requests, supplier
  reservations, idempotency records, incidents, bookings; re-seeds; clears supplier in-memory state;
  returns the system to exactly `CT-DEMO-78291 CONFIRMED`. **Must be reliable — it runs live mid-demo.**
- **View Technical Details** (demo mode only): correlation ID, state, supplier outcome, event timeline.

---

## 10. API surface

```
GET  /api/bookings/{confirmation}
POST /api/bookings/{confirmation}/change/availability
POST /api/bookings/{confirmation}/change            (Idempotency-Key header)
GET  /api/change-requests/{id} · /events
POST /api/change-requests/{id}/reconcile · /incident
GET  /api/ops/transactions · /transactions/{correlationId} · /supplier-reliability
POST /api/demo/scenario · /api/demo/reset
```

Suppliers: `GET|POST|DELETE /api/hotels/availability|reservations[/{id}]`, same for `/api/cars/...`,
plus `GET /api/cars/reservations/by-client-reference/{ref}`.

DTO records + Bean Validation. `@RestControllerAdvice` returns `{ "code", "message", "correlationId" }` —
404 not found · 409 invalid state / duplicate / in progress · 400 bad input · 502/503 translated supplier
failure · 500 unexpected. No stack traces to the browser.

`GET /api/ops/metrics` and **Swagger are cut** — Postman covers API demonstration at a fraction of the cost.

---

## 11. Security scope

`DemoMemberContext` always resolves the seeded member; authentication is explicitly out of scope and
documented. No credential form, no payment form, no card number, ever. `/ops` labeled "Internal Demo"
with no false security claims. No secrets in the repo — `.env.example` only, dev-only SA password from
environment.

**ServiceNow:** `IncidentService` + `MockServiceNowIncidentClient` creating persisted `INC-DEMO-####`
incidents, wired to the ops page. README: the mock is intentional (no access to Costco's ServiceNow);
the interface isolates the provider so a real REST adapter drops in.

---

## 12. Testing — ~10–12 meaningful tests

JUnit 5 + Mockito + Spring Boot Test. Must actually pass.

1. successful replacement 2. hotel reservation failure 3. **car timeout + compensation**
4. **original stays CONFIRMED when replacement fails** 5. idempotent duplicate
6. same key + different payload → 409 7. ambiguous timeout → RECONCILIATION_REQUIRED
8. reconciliation flow 9. invalid original booking 10. concurrent change → 409
11. price comparison math 12. supplier client error mapping

Integration (`@Tag("integration")`, failsafe, real SQL Server): migrations apply, seed correct,
stored procedure returns rows via JDBC, happy path end-to-end.

Service-layer depth over coverage percentage. No coverage target.

---

## 13. Cheap JD checkboxes — keep

Each answers a JD line item directly for roughly thirty minutes of total work:

- **Jenkinsfile** — Checkout → Build → Unit Test → Integration Test → Package → Archive →
  clearly non-production Deploy. Generic env vars; no invented Costco infrastructure.
- **Postman collection** — `postman/Costco-Travel-Smart-Rebook.postman_collection.json` + environment:
  get booking, check availability, submit change, duplicate request, supplier availability,
  ops transactions, supplier reliability, reconcile, create incident.
- **T-SQL stored procedure + JDBC repository** — already in §5.

---

## 14. Docs — the highest-leverage artifact after the orchestrator

- **`docs/TECHNICAL_QA.md`** — *write this first among the docs.* Conversational answers to the
  20 hiring-manager questions, including the honest ones: "I don't know Costco Travel's actual
  architecture; this is one possible design based on public product behavior." This document is what
  carries the twenty minutes after the demo.
- **`README.md`** — description · Why I Built This (respectful framing) · Core Invariant ·
  Architecture (Mermaid) · Happy Path + Car Timeout + Ambiguous Outcome (Mermaid sequences) ·
  Idempotency · Transaction Boundaries · Data Model · SQL/Hibernate/JDBC decisions · Observability ·
  CI/CD · Testing · Tradeoffs · **What Is Mocked** (hotel, car, ServiceNow, payments, auth) ·
  **What Is Real** (Spring Boot services, REST, SQL persistence, JPA, JDBC reporting, T-SQL proc,
  idempotency, workflow states, compensation, timeouts, reconciliation, structured logs, tests, Jenkins) ·
  How to Run · What I'd Explore With Real Domain Context.
- **`docs/DEMO_SCRIPT.md`** — the run-of-show in §16, with the exact words to say.
- **`docs/TALKING_POINTS.md`** — the "why" questions, condensed.

---

## 15. Execution phases

| Phase | Work | Gate |
|---|---|---|
| 0 | `git init`, `.gitignore`, Maven Wrapper, root `pom.xml`, `.env.example`, start Docker | `./mvnw -v` and `docker info` succeed |
| 1 | compose + SQL Server; Flyway V1–V4 | Schema + seed verified by query |
| 2 | `shared-contracts`; hotel + car suppliers with scenario behavior | `curl` availability/reserve/cancel on :8081, :8082 |
| 3 | Entities, repositories, JDBC repo, Feign clients, Resilience4j | `./mvnw test` green |
| 4 | **Orchestrator, idempotency, compensation, reconciliation, exception handler** | Tests 1–12 green |
| 5 | JSP demo path + CSS + vanilla JS | Full member flow renders, no console errors |
| 6 | Ops page (timeline + reliability table), ServiceNow mock | Real data from SQL Server/JDBC |
| 7 | Demo controls + reset, structured logging, actuator, startup banner | Car-timeout scenario reproducible end-to-end |
| 8 | Integration tests, Postman, Jenkinsfile | `./mvnw clean verify` green |
| 9 | Docs (TECHNICAL_QA first), one styling pass at 1440px, final sweep | §17 satisfied |

Phases 3–4 carry the interview. If anything slips, it slips from 5–6, never from 4.

Build cost control: pipe Maven/test output to files and grep rather than dumping logs into context;
route Phases 2, 5, and 8 to Sonnet subagents where useful.

---

## 16. Demo run-of-show (5–10 minutes)

| Time | Action | What you say |
|---|---|---|
| 0:00 | `/` → **View Upcoming Trip** → booking detail | "I explored the public Costco Travel flow and got curious about one workflow." |
| 0:45 | **Change Trip** → dates Mar 15–20 → **Check Availability** | "That's two real HTTP calls to separate supplier services." |
| 1:30 | Comparison, +$70 → **Replace Reservation** → confirm dialog | "This line is the invariant: the existing reservation stays active until the replacement is secured." |
| 2:15 | Success — old cancelled, new `CT-DEMO-89412` confirmed | |
| 2:45 | **Reset Demo** | |
| 3:00 | Demo Controls → **Car Timeout** → repeat the change | |
| 4:00 | Failure notice — **original still confirmed** | "The car supplier timed out after the hotel was already reserved. It compensated and preserved the original booking." |
| 4:30 | `/ops` → transaction → **timeline from SQL Server** | "Every one of those rows is a persisted event, correlated by ID across all three services." |
| 5:30 | Supplier reliability table | "That table comes from a T-SQL stored procedure called over JDBC." |
| — | *Backup only if asked* | Ambiguous supplier result → reconciliation. "A timeout isn't a confirmed failure." |

Record a screen capture as a fallback before the interview. Live demos fail.

---

## 17. Definition of Done

**Must be verified by running it:**
Maven build succeeds · unit tests pass · SQL Server schema initializes · seed data exists ·
home → bookings → `CT-DEMO-78291` → change page all load · availability comes from supplier HTTP ·
`$2,410` and `+$70` appear · happy-path replacement succeeds · old booking cancels **only after**
replacement confirmation · **demo reset restores original data reliably** · **car timeout causes a real
backend timeout** · **compensation executes** · **original stays CONFIRMED** · ops timeline renders from
persisted events · reliability report comes from JDBC + stored procedure · JSON logs carry correlation IDs.

**Must exist and be correct:** idempotency + concurrency + ambiguous/reconciliation code paths with
passing tests · ServiceNow mock incident · Jenkinsfile · Postman collection · README · TECHNICAL_QA ·
DEMO_SCRIPT · TALKING_POINTS.

**Hygiene:** no secrets committed · no `TODO`/`FIXME`/`UnsupportedOperationException` in essential paths ·
formatted dates and currency · loading states · buttons disabled during requests · no horizontal overflow ·
no broken images · no significant browser console errors.

Nothing is claimed to work unless it was actually run and observed. Failures are reported plainly.

---

## 18. Run commands (target state)

```bash
cp .env.example .env          # dev-only SA password
docker compose up -d          # SQL Server (Docker Desktop must be running)
./mvnw clean verify           # build + unit tests
./scripts/start-demo.sh       # or scripts/start-demo.ps1 on Windows
```

| Surface | URL |
|---|---|
| Member portal | http://localhost:8080 |
| Operations | http://localhost:8080/ops |
| Hotel / car supplier health | http://localhost:8081/actuator/health · :8082 |

Demo booking **CT-DEMO-78291** · Reset via Demo Controls → Reset Demo, or `POST /api/demo/reset`.
