# Technical Q&A

Conversational answers to twenty questions this project tends to raise, grounded in what's actually
built.

### 1. Walk me through the architecture.

There are three Spring Boot services and a shared DTO module in one Maven reactor. `booking-service`
runs on port 8080, is packaged as a WAR because it serves JSP views alongside its REST API, and owns the
orchestration, persistence, and the member-facing UI. `hotel-supplier-service` (8081) and
`car-supplier-service` (8082) are independent Spring Boot jar applications that simulate a hotel and car
rental supplier — in-memory reservation stores, deterministic pricing, and demo-scenario-driven failure
behavior. `booking-service` talks to both over real HTTP via Feign clients (`HotelSupplierClient`,
`CarSupplierClient`). All persistent state lives in one SQL Server database, managed by Flyway
migrations, written through Hibernate/JPA and read for the reliability report through a JDBC repository
calling a stored procedure.

### 2. Why did you use microservices?

Mainly because the whole point of the project is what happens when an operation depends on two systems
you don't control the timing or reliability of. If the hotel and car "suppliers" were just Java classes
in the same process, I couldn't honestly demonstrate a network timeout, a circuit breaker tripping, or an
ambiguous supplier outcome — those things only exist because there's a real HTTP boundary in between.

### 3. Why not cancel the current reservation first?

Because that's exactly the failure mode the core invariant exists to prevent. If you cancel first and the
replacement then fails, the member is left with nothing. `RebookOrchestrator` always reserves the
replacement hotel and car first, and only calls `compensateHotel()`/cancels the original after both
replacement reservations are confirmed — the sequence in the happy path is reserve hotel, reserve car,
*then* cancel the original hotel and car reservations, then flip the old booking to `CANCELLED` and the
new one to `CONFIRMED`.

### 4. What happens if the hotel books but the car doesn't?

The car call fails outright (say, a `HOTEL_FAILURE`-style 503 equivalent, or a genuine `CAR_TIMEOUT`).
The orchestrator's compensation path calls `compensateHotel()` — a real `DELETE` against the hotel
supplier's reservation — to undo the hotel side, then leaves the original booking exactly as it was:
`CONFIRMED`. The change request is marked `FAILED`. Nothing about the member's original trip changes.

### 5. What happens if the supplier processed the booking but timed out before responding?

That's the `CAR_AMBIGUOUS` scenario, and it's the case I care most about explaining well. My HTTP client
gives up after a 3-second read timeout, but the car supplier's simulated processing can take up to 8
seconds — so by the time I've timed out, the supplier might still go on to actually create the
reservation. At the HTTP level I get the exact same `SupplierTimeoutException` whether that happens or
not. Rather than guess, the orchestrator treats it the same as a hard failure in the moment — compensate
the hotel side, preserve the original booking — but sets `reconciliation_status = REQUIRED` on the change
request instead of `NOT_REQUIRED`. Later, the `ReconciliationService` calls the car supplier's
`GET /api/cars/reservations/by-client-reference/{reference}` endpoint — a lookup keyed by the reference
*I* generated, not one the supplier returned — to find out, after the fact, whether an orphan reservation
actually exists, and cancels it if so.

### 6. Why can't a SQL transaction solve this?

Because a SQL transaction only spans the database connection it's running on — it has no way to reach
into an independent HTTP service and undo an action that service already took. My local
`@Transactional` boundaries protect consistency within `booking-service`'s own tables, but if the hotel
supplier has already persisted a reservation in its own in-memory store, no `ROLLBACK` on my side touches
that. That's why the README states it plainly: a relational transaction protects local state, not
external supplier systems.

### 7. Explain your idempotency implementation.

`POST /api/bookings/{confirmation}/change` requires an `Idempotency-Key` header. `IdempotencyService`
computes a SHA-256 hash of the request body and looks up `idempotency_records` by key. If there's no
existing record, it proceeds and, on completion, stores the key, hash, operation name, status, and the
serialized response body (that column is `VARCHAR(MAX)` specifically because Hibernate's `@Lob String`
maps to a CLOB, and SQL Server's dialect expects `varchar(max)` for that, not `nvarchar(max)`). If a
record already exists with the same key and the same hash, the stored response is replayed verbatim with
zero supplier calls.

### 8. What if the same idempotency key has a different request body?

That's a real conflict, not a replay — different hash, same key. The service returns `409
IDEMPOTENCY_KEY_CONFLICT` via the shared `ApiError` shape (`code`, `message`, `correlationId`). The intent
is that a key represents one specific attempted operation; reusing it for a materially different request
is a client bug, not something to silently honor.

### 9. Where did you use Hibernate?

Everywhere writes happen: `Booking`, `BookingItem`, `ChangeRequest`, `SupplierReservation`,
`BookingEvent`, `IdempotencyRecord`, and `Incident` are all JPA entities with Spring Data repositories.
`Booking` carries a `@Version` column for optimistic locking, which is what turns a second concurrent
change attempt on the same booking into a clean `409` instead of a silent lost update.

### 10. Where did you use JDBC?

`SupplierReliabilityJdbcRepository`, using `JdbcTemplate` directly, for exactly one query: calling
`sp_supplier_reliability_report` and mapping the result set to `SupplierReliabilityRow`. That's the one
place in the app where I intentionally stepped outside Hibernate.

### 11. Why use a stored procedure?

Because `PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY latency_ms) OVER (PARTITION BY supplier)` is a
window function, and computing a p95 latency per supplier over an arbitrary, potentially large set of
`booking_events` rows is exactly the kind of aggregation the database engine should do, not something I
want to pull into the JVM and compute by hand. It's also a much more honest demonstration of real SQL
skill than an ORM query would be — and it forced me to actually learn that `PERCENTILE_CONT` isn't a
`GROUP BY` aggregate in T-SQL, so it has to be computed per-row in a CTE and then collapsed with `MAX` in
the outer `GROUP BY`.

### 12. Explain the indexes.

Every index in `V2__create_indexes.sql` is tied to a specific query, not added speculatively.
`ix_booking_events_supplier_status_created` on `(supplier, status, created_at)` backs the reliability
procedure's filtering and ordering. `ix_booking_events_correlation_id` backs every ops timeline lookup,
which fetches all events for one correlation ID. The unique index on `bookings.confirmation_number` backs
the primary member lookup, `GET /api/bookings/{confirmation}`. The unique constraint on
`idempotency_records.idempotency_key` is the actual correctness mechanism, not just a performance index.
And `ix_change_requests_booking_status` on `(booking_id, status)` backs the "is there already an
in-progress change for this booking" guard that produces the concurrency `409`.

### 13. How would you diagnose a production issue?

Start from the correlation ID — either from a member complaint tied to a timeframe, or from an alert.
Every log line across all three services for that change request carries the same `correlationId` in its
MDC fields, so I'd pull those first. Then I'd check `/api/ops/transactions/{correlationId}` for the
persisted event timeline — that's the same data the ops UI renders, just as JSON — to see exactly which
step failed and what the recorded latency was. If it's an ambiguous-timeout case, the reconciliation
status on that change request tells me immediately whether it's already been resolved or still needs
attention.

### 14. How would Splunk fit?

The `SplunkHecAppender` is already wired into `logback-spring.xml` — it forwards the same structured JSON
logs to a Splunk HTTP Event Collector, and it's completely inert unless `SPLUNK_HEC_URL` and
`SPLUNK_HEC_TOKEN` are set. I don't have a real Splunk instance for this project, so I built it to the
interface without claiming a live integration — the point was to show the logs are already shaped
correctly for that kind of forwarding, not to fake a working dashboard.

### 15. What does the Jenkins pipeline do?

Checkout, then `./mvnw -B clean compile`, then `./mvnw -B test` with JUnit results published via the
`junit` step, then an integration test stage that's present but gated behind an environment variable
because those tests need a live SQL Server (the same one docker-compose stands up) rather than an
in-memory substitute, then `./mvnw -B package -DskipTests`, a lightweight verification stage, artifact
archiving for the built WAR and jars, and finally a clearly-labeled, clearly-non-production Deploy stage
that just describes what a real deploy would do.

### 16. How would you deploy this?

Realistically, for a prototype like this, `scripts/start-demo.sh` / `start-demo.ps1` running the built
artifacts locally is the actual deployment story, and I'm upfront about that. If I were deploying this for
real, I'd containerize each of the three services, put them behind whatever the organization's actual
ingress/load-balancing story is, externalize the SQL Server connection to a managed instance, and swap the
Jenkins Deploy placeholder for a real push to an artifact repository and a rollout step — none of which I
built here because I don't have a real target environment to deploy into.

### 17. What tests did you write?

Unit-level orchestration tests are the core of it: a successful replacement, a hotel reservation failure,
the car-timeout-plus-compensation path, a check that the original booking stays `CONFIRMED` when a
replacement fails, an idempotent duplicate request, a same-key-different-body conflict, the ambiguous
timeout landing in `RECONCILIATION_REQUIRED`, the reconciliation flow resolving it, an invalid original
booking being rejected, a concurrent change producing a `409`, the price-comparison math, and supplier
client error mapping. Integration tests tagged `@Tag("integration")` run against a real SQL Server under
a `test` profile via failsafe, covering migrations applying cleanly, the seed data being correct, the
stored procedure returning rows through JDBC, and the full happy path end-to-end.

### 18. What if two users/requests try to change the reservation simultaneously?

`Booking.version` is a JPA `@Version` column. The second concurrent write to the same booking row fails
optimistic locking, and the orchestrator translates that into a `409` with a message like "another change
is already being processed for this reservation," backed by the `ix_change_requests_booking_status` index
that lets the in-progress check itself run fast.

### 19. What if compensation fails?

I'll be honest — that's the least polished corner of this prototype. If the compensating `DELETE` call
itself fails, the change request lands in a reconciliation-required state, the same mechanism used for
ambiguous timeouts, rather than silently swallowing the error. What I haven't built is a durable
retry-with-backoff around compensation calls specifically; at production scale I'd want that to go through
a queue that guarantees eventual delivery rather than a single synchronous attempt in the request path.

### 20. How would this architecture change at 100x scale?

A few things would have to change. The synchronous, in-request orchestration across two supplier calls
would likely become asynchronous, with compensation and reconciliation driven off a durable event log or
queue rather than direct HTTP calls in the critical path. I'd want real observability — dashboards and
SLOs, not a single ops page — and probably a dedicated reconciliation worker rather than an
operator-triggered button. The database access pattern would need real load testing; the stored procedure
approach for the reliability report is fine at demo scale but would need to be re-evaluated under real
event volume. And honestly, at that scale I'd want to validate a lot of these assumptions against
whatever the real supplier contracts and internal platform constraints actually are, rather than the ones
I inferred from the outside.
