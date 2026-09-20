package com.smartrebook.booking.demo;

import com.smartrebook.booking.client.CarSupplierClient;
import com.smartrebook.booking.client.HotelSupplierClient;
import com.smartrebook.contracts.DemoScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs live during demos, so this must be reliable: direct JDBC truncate + deterministic
 * reseed (mirrors V4__seed_demo_data.sql) rather than relying on Hibernate cascades, plus
 * best-effort resets of each supplier's in-memory state and the demo scenario toggle.
 */
@Service
public class DemoResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoResetService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DemoScenarioState demoScenarioState;
    private final HotelSupplierClient hotelSupplierClient;
    private final CarSupplierClient carSupplierClient;

    public DemoResetService(JdbcTemplate jdbcTemplate, DemoScenarioState demoScenarioState,
                             HotelSupplierClient hotelSupplierClient, CarSupplierClient carSupplierClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.demoScenarioState = demoScenarioState;
        this.hotelSupplierClient = hotelSupplierClient;
        this.carSupplierClient = carSupplierClient;
    }

    @Transactional
    public void reset() {
        jdbcTemplate.update("DELETE FROM booking_events");
        jdbcTemplate.update("DELETE FROM supplier_reservations");
        jdbcTemplate.update("DELETE FROM change_requests");
        jdbcTemplate.update("DELETE FROM idempotency_records");
        jdbcTemplate.update("DELETE FROM incidents");
        jdbcTemplate.update("DELETE FROM booking_items");
        jdbcTemplate.update("DELETE FROM bookings");
        jdbcTemplate.update("DELETE FROM members");

        jdbcTemplate.update(
                "INSERT INTO members (member_reference, first_name, last_name, membership_type) VALUES (?, ?, ?, ?)",
                "DEMO-EXEC-001", "Sainath", "Gandhe", "Executive Member");

        jdbcTemplate.update(
                "INSERT INTO bookings (confirmation_number, member_id, destination, check_in_date, check_out_date, total_amount, currency, status, version) " +
                        "SELECT ?, id, ?, ?, ?, ?, 'USD', 'CONFIRMED', 0 FROM members WHERE member_reference = ?",
                "CT-DEMO-78291", "Maui, Hawaii", "2027-03-14", "2027-03-19", 2340.00, "DEMO-EXEC-001");

        insertItem("HOTEL", "HOTEL_SUPPLIER", "Wailea Beach Resort - Ocean View King", 1780.00, "HTL-DEMO0001");
        insertItem("CAR", "CAR_SUPPLIER", "Standard SUV", 360.00, "CAR-DEMO0001");
        insertItem("FEES", "INTERNAL", "Taxes & Fees", 200.00, null);
        insertItem("MEMBER_BENEFIT", "INTERNAL", "$200 Digital Costco Shop Card", 0.00, null);

        demoScenarioState.set(DemoScenario.NORMAL);

        resetSupplierBestEffort("hotel", hotelSupplierClient::resetDemoState);
        resetSupplierBestEffort("car", carSupplierClient::resetDemoState);
    }

    private void insertItem(String itemType, String supplier, String description, double amount, String supplierConfirmation) {
        jdbcTemplate.update(
                "INSERT INTO booking_items (booking_id, item_type, supplier, description, amount, supplier_confirmation, status) " +
                        "SELECT id, ?, ?, ?, ?, ?, 'CONFIRMED' FROM bookings WHERE confirmation_number = 'CT-DEMO-78291'",
                itemType, supplier, description, amount, supplierConfirmation);
    }

    private void resetSupplierBestEffort(String name, Runnable resetCall) {
        try {
            resetCall.run();
        } catch (Exception e) {
            log.warn("Could not reset {} supplier demo state: {}", name, e.getMessage());
        }
    }
}
