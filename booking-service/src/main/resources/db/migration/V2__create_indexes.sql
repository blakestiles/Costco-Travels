-- Every index below is justified by a concrete query path (see README: SQL/Hibernate/JDBC Decisions).
-- No decorative indexes.

-- Drives sp_supplier_reliability_report (filters by supplier+status, orders by created_at
-- for the latency percentile window) and the ops "recent supplier activity" queries.
CREATE INDEX ix_booking_events_supplier_status_created
    ON booking_events (supplier, status, created_at);

-- Every timeline/transaction-detail lookup in the ops UI fetches all events for one correlation id.
CREATE INDEX ix_booking_events_correlation_id
    ON booking_events (correlation_id);

-- Primary member-facing lookup: GET /api/bookings/{confirmation}.
CREATE UNIQUE INDEX uq_bookings_confirmation_number
    ON bookings (confirmation_number);

-- The idempotency guarantee itself is only as fast as this lookup on the hot path of every change request.
-- (Also enforced as a UNIQUE CONSTRAINT in V1; this index name documents the intent explicitly.)
-- No additional index needed here since the V1 unique constraint already creates one.

-- Guards the "another change is already in progress for this booking" check before starting a new one.
CREATE INDEX ix_change_requests_booking_status
    ON change_requests (booking_id, status);
