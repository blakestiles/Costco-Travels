# Demo Walkthrough

A guided walkthrough of the core flow, under 4 minutes, with an optional appendix covering the
ambiguous-timeout/reconciliation path. Recording a screen capture beforehand as a fallback is a good
idea — live demos are always a little riskier than a recording.

Before starting: run `./scripts/start-demo.sh` (or `start-demo.ps1`), confirm all three services are
healthy, and open http://localhost:8080 in a browser at 1440px.

## Core path (under 4 minutes)

**1. Open the booking (0:00)**
Click **View Upcoming Trip** on the home page, or navigate to **My Trips** and open booking
`CT-DEMO-78291`.

> "This is a prototype built around a public workflow on the Costco Travel site — changing the dates on
> an existing hotel-and-car booking — used as a starting point to dig into a distributed-systems
> reliability problem. It's a prototype exploring that problem, not a copy of their system."

**2. Change the trip dates (0:30)**
Click **Change Trip**. The dates default to March 15–20, 2027. Click **Check Availability**.

> "That button just made two real HTTP calls — one to a hotel supplier service, one to a car supplier
> service, both separate Spring Boot applications running on their own ports."

**3. Review the comparison (1:15)**
The page shows the current trip ($2,340) against the proposed trip ($2,410), a $70 difference.

> "The new dates cost seventy dollars more. Nothing has been booked yet — this was a read-only check."

Click **Replace Reservation**. A confirmation dialog appears.

> "This line right here is the whole point of the project: *your existing reservation will remain active
> until the replacement is successfully secured.* The system won't touch your current booking until the
> new one is fully confirmed."

Confirm the dialog.

**4. Success (2:00)**
The page shows the new confirmation, `CT-DEMO-89412`, confirmed. The original booking is now cancelled.

> "Behind the scenes, that reserved a new hotel room, then a new car, then — only after both succeeded —
> cancelled the original hotel and car reservations and marked the old booking cancelled. If either
> supplier call had failed, none of that cancellation would have happened."

**5. Reset the demo (2:30)**
Click **Reset Demo** (or use the Demo Controls panel) and confirm.

> "That restores everything back to the original seeded state — bookings, events, supplier data, all of
> it — so I can show the failure case cleanly."

**6. Trigger the car timeout scenario (2:45)**
Open **Demo Controls**, select **Car Timeout**, then repeat the change: **Change Trip** → same dates →
**Check Availability** → **Replace Reservation** → confirm.

> "I've just told the car supplier service to simulate a slow response — it'll take longer than the
> three-second timeout my client is configured to wait for."

**7. Show the failure and preserved original (3:30)**
The UI shows a plain failure message — no stack traces, no HTTP codes.

> "It says: 'We couldn't complete your requested change. Your existing reservation is still confirmed.'
> The hotel replacement had already gone through when the car call timed out, so the system compensated —
> it cancelled that replacement hotel reservation automatically — and the member's original booking never
> changed state. That's the invariant: no reservation change knowingly leaves the member without their
> original, valid booking."

**8. Open Operations and the transaction timeline (4:00 — if time allows)**
Navigate to `/ops`, open the transaction for this change request.

> "Every one of these rows is a persisted event in SQL Server — request received, hotel reserved, car
> timeout, compensation started, hotel replacement cancelled, original preserved, change failed — all
> tagged with the same correlation ID, so I can trace one change request across every service and every
> log line."

**9. Show the supplier reliability table (5:00 — if time allows)**

> "This table is sourced from a T-SQL stored procedure — it computes p95 latency per supplier using a
> window function, called from a JDBC repository rather than through Hibernate, because this is exactly
> the kind of database-native reporting query JPA isn't a good fit for."

## Appendix — Ambiguous Supplier Response + Reconcile (only if asked, or time remains)

**10. Reset the demo again.**

**11. Demo Controls → Car Ambiguous Response.** Repeat the change flow.

> "This time the car supplier *does* create the reservation, it just responds after my client has already
> given up and timed out. From the booking service's point of view, this looks identical to the previous
> scenario — a timeout is a timeout at the HTTP layer. So it does the same safe thing: compensates the
> hotel side, preserves the original booking, and fails the change request. But it also flags this one for
> reconciliation, because there's now an orphaned reservation sitting on the car supplier's side that
> nobody's cleaned up yet."

**12. Open Operations, find the change request, click Reconcile.**

> "Reconcile calls the car supplier back — not to retry the booking, just to ask 'did you actually create
> something for this reference?' It finds the orphan, cancels it, and closes out the reconciliation. The
> takeaway is: a timeout is never treated as a confirmed failure. The system never guesses; it always
> checks."
