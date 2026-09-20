package com.smartrebook.booking.repository.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Direct JDBC, not Hibernate: this is a pure read model over a T-SQL stored procedure
 * (sp_supplier_reliability_report, see V3 migration) that uses window-function analytics
 * (PERCENTILE_CONT) with no corresponding JPA entity. Mapping this through JPA would mean
 * either faking an entity for a query result or dropping into native SQL anyway - JdbcTemplate
 * is the more honest tool for a report that is not part of the write-side domain model.
 * See README: SQL/Hibernate/JDBC Decisions.
 */
@Repository
public class SupplierReliabilityJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public SupplierReliabilityJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SupplierReliabilityRow> fetchReliabilityReport() {
        return jdbcTemplate.query(
                "EXEC sp_supplier_reliability_report",
                (rs, rowNum) -> new SupplierReliabilityRow(
                        rs.getString("supplier_name"),
                        rs.getLong("booking_attempts"),
                        rs.getLong("success_count"),
                        rs.getLong("failure_count"),
                        rs.getBigDecimal("success_rate"),
                        rs.getLong("average_latency_ms"),
                        rs.getLong("p95_latency_ms")
                )
        );
    }
}
