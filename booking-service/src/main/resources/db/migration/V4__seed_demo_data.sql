-- Deterministic demo seed. POST /api/demo/reset re-runs the equivalent of this data
-- (via DemoResetService, not by re-running Flyway) to restore exactly this state.

INSERT INTO members (member_reference, first_name, last_name, membership_type)
VALUES ('DEMO-EXEC-001', 'Sainath', 'Gandhe', 'Executive Member');

INSERT INTO bookings (confirmation_number, member_id, destination, check_in_date, check_out_date, total_amount, currency, status, version)
SELECT 'CT-DEMO-78291', id, 'Maui, Hawaii', '2027-03-14', '2027-03-19', 2340.00, 'USD', 'CONFIRMED', 0
FROM members WHERE member_reference = 'DEMO-EXEC-001';

INSERT INTO booking_items (booking_id, item_type, supplier, description, amount, supplier_confirmation, status)
SELECT b.id, 'HOTEL', 'HOTEL_SUPPLIER', 'Wailea Beach Resort - Ocean View King', 1780.00, 'HTL-DEMO0001', 'CONFIRMED'
FROM bookings b WHERE b.confirmation_number = 'CT-DEMO-78291';

INSERT INTO booking_items (booking_id, item_type, supplier, description, amount, supplier_confirmation, status)
SELECT b.id, 'CAR', 'CAR_SUPPLIER', 'Standard SUV', 360.00, 'CAR-DEMO0001', 'CONFIRMED'
FROM bookings b WHERE b.confirmation_number = 'CT-DEMO-78291';

INSERT INTO booking_items (booking_id, item_type, supplier, description, amount, supplier_confirmation, status)
SELECT b.id, 'FEES', 'INTERNAL', 'Taxes & Fees', 200.00, NULL, 'CONFIRMED'
FROM bookings b WHERE b.confirmation_number = 'CT-DEMO-78291';

INSERT INTO booking_items (booking_id, item_type, supplier, description, amount, supplier_confirmation, status)
SELECT b.id, 'MEMBER_BENEFIT', 'INTERNAL', '$200 Digital Costco Shop Card', 0.00, NULL, 'CONFIRMED'
FROM bookings b WHERE b.confirmation_number = 'CT-DEMO-78291';
