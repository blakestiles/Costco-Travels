# Talking Points

Short, conversational answers to the "why" questions that tend to come up in the room, not the deep
technical walkthroughs (those are in `TECHNICAL_QA.md`).

**Why Spring Boot?**
It's the framework I associate most with what a real enterprise Java backend team is actually running,
and it gets you production-shaped defaults — actuator health checks, embedded Tomcat, configuration
binding — without hand-rolling infrastructure. For a project meant to demonstrate backend engineering
judgment rather than framework novelty, that felt like the right choice.

**Why JSP instead of React?**
Two reasons, honestly. First, the plan was explicit that screen time isn't the product here — the
orchestration and persistence layer is what earns interview credibility, not the frontend. Second, JSP
plus a REST API is a very real, very common enterprise pattern, and it let me spend the frontend budget
on "immediately familiar," not "pixel-perfect," while putting the real effort into the parts that survive
a follow-up question.

**Why JPA and JDBC, not just one?**
Because they're good at different things. JPA/Hibernate is great for entity writes with relationships and
optimistic locking — that's most of this app. But the supplier reliability report is a database-native
percentile calculation across historical rows, which is exactly the kind of thing you write as SQL and
call directly, not the kind of thing you want an ORM abstracting for you. Using JDBC there isn't a
compromise, it's the right tool.

**Why SQL Server?**
Mostly because that's what I assumed I'd want to be comfortable with for this kind of role, and I wanted
a real relational database with a real T-SQL stored procedure in the demo, not an in-memory substitute.
It also forced me to deal with some genuinely SQL-Server-specific gotchas — `DATETIMEOFFSET` vs
`DATETIME2`, `PERCENTILE_CONT` being window-only — that were worth learning the hard way.

**Why idempotency?**
Because a member's "confirm this change" click can get retried — a flaky network, a double-click, a
browser retry — and a retried POST against an orchestration that reserves hotel and car rooms should
never create a second reservation. The `Idempotency-Key` plus a hash of the request body means a retry
replays the original outcome instead of re-running the whole thing.

**Why not blindly retry a failed POST?**
Because a POST that creates a reservation isn't safe to retry blindly — if the first attempt actually
succeeded and you just didn't get the response, retrying creates a duplicate reservation. That's exactly
the ambiguous-timeout problem the whole project is built around. GET calls (availability checks) get
bounded retries because they're safe to repeat; reservation-creating POSTs never do.

**Why compensation instead of relying on a database rollback?**
Because the two suppliers are independent systems reachable only over HTTP — there's no shared
transaction to roll back. If the hotel side succeeds and the car side fails, the only way to undo the
hotel side is to actually call it again and cancel it. That's compensation, not rollback.

**What happens if compensation itself fails?**
Then the change request goes into a state that flags it for reconciliation rather than silently failing.
The honest answer is I haven't built infinite retry-with-backoff around compensation calls — at prototype
scale, a failed compensation call surfaces as a reconciliation case and, realistically, an incident. At
production scale I'd want a durable retry queue for compensating actions specifically, since those are
the calls you really can't afford to just drop.

**Why reconciliation as a separate step instead of trying to resolve everything inline?**
Because when a timeout is genuinely ambiguous — the supplier may or may not have created something — the
correct move in the request/response path is to not guess. Reconciliation is a deliberate, out-of-band
step that asks the supplier "did you actually do this?" instead of the orchestrator assuming an answer
under time pressure.

**Why structured logs?**
Because "grep the console" doesn't scale past one developer on one laptop. JSON logs with consistent
fields mean you can actually query them — by correlation ID, by supplier, by event type — the moment you
have more than a trivial amount of log volume.

**Why correlation IDs?**
Because one member action fans out into calls across three services. Without a correlation ID threading
through all of them, reconstructing what happened during an incident means manually correlating
timestamps across three separate log streams, which is miserable and error-prone.

**Why a stored procedure instead of doing the aggregation in Java?**
Because a percentile-over-partition calculation across potentially large volumes of event rows is a job
the database is built to do efficiently, and pulling all those rows into the JVM to compute it there would
be strictly worse. It's also a concrete demonstration that I can write real T-SQL, not just call an ORM.

**What would change at production scale?**
A lot. Real supplier contracts instead of mocks, a durable message queue for compensation/reconciliation
retries instead of synchronous HTTP in the request path, real observability and SLOs instead of a single
ops page, and probably reconsidering whether two supplier calls in one request thread is even the right
shape once you're not doing a live demo.

**Why not distributed database transactions (two-phase commit) across the suppliers?**
Because the suppliers are external systems I don't control and can't assume support XA or any two-phase
commit protocol — and even if they did, 2PC has real availability costs (a coordinator failure can leave
participants blocked holding locks). Compensation is the pattern that works when you don't get to assume
cooperative transaction support from the other side.

**Why not Kafka?**
For this project's scope, the entire orchestration happens synchronously within a single member-initiated
request, and there's no need for a broker or event stream to make that work. If I were building this at
real scale, I'd likely want an async event backbone for compensation/reconciliation retries — but
reaching for Kafka here would have added infrastructure complexity without adding a proportional amount
of correctness or demonstrable engineering value.

**Why not build this as one monolith?**
The hotel and car "suppliers" specifically needed to behave like independent external systems — with
their own HTTP boundary, their own failure modes, their own timeouts — because the entire point of the
project is reasoning about what happens when you don't control or trust another service's timing. A
monolith with three modules calling each other in-process couldn't honestly demonstrate a network timeout.

**A few honest caveats, up front:**
I don't know Costco Travel's actual architecture, supplier contracts, or internal constraints — this is
one possible design based on observing the public product, not a reverse-engineering of their system.
Before proposing any of this for a real production environment, I'd want to understand the real supplier
contracts, the real failure modes those suppliers actually exhibit, and the operational constraints a team
that owns this in production is already living with.
