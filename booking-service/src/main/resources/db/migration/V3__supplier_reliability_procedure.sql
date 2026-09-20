-- Convention this procedure depends on: every supplier HTTP call made by the orchestrator
-- (RebookOrchestrator.reserveHotel/reserveCar) writes exactly one booking_events row with
-- event_type = 'SUPPLIER_CALL', a non-null supplier ('HOTEL' or 'CAR'), a status of
-- 'SUCCESS' | 'FAILURE' | 'TIMEOUT', and the measured latency_ms. This procedure is the
-- JDBC-backed read model for the Operations "Supplier Reliability" table.
--
-- Note: PERCENTILE_CONT in SQL Server is a window-only function (WITHIN GROUP ... OVER),
-- not a GROUP BY aggregate, so it is computed per-row in a CTE and collapsed with MAX/GROUP BY
-- in the outer query.

CREATE OR ALTER PROCEDURE sp_supplier_reliability_report
AS
BEGIN
    SET NOCOUNT ON;

    WITH supplier_calls AS (
        SELECT
            supplier,
            status,
            latency_ms,
            PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY CAST(latency_ms AS FLOAT))
                OVER (PARTITION BY supplier) AS p95_latency_ms
        FROM booking_events
        WHERE event_type = 'SUPPLIER_CALL'
          AND supplier IS NOT NULL
          AND latency_ms IS NOT NULL
    )
    SELECT
        supplier AS supplier_name,
        COUNT(*) AS booking_attempts,
        SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
        SUM(CASE WHEN status <> 'SUCCESS' THEN 1 ELSE 0 END) AS failure_count,
        CAST(ROUND(100.0 * SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*), 2) AS DECIMAL(5,2)) AS success_rate,
        CAST(ROUND(AVG(CAST(latency_ms AS FLOAT)), 0) AS BIGINT) AS average_latency_ms,
        CAST(ROUND(MAX(p95_latency_ms), 0) AS BIGINT) AS p95_latency_ms
    FROM supplier_calls
    GROUP BY supplier
    ORDER BY supplier;
END
GO
