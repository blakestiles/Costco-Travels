-- Core domain schema for Costco Travel Smart Rebook.
-- Local relational transactions protect this schema's own consistency only; they cannot
-- atomically span the external hotel/car supplier systems (see README: Transaction Boundaries).

CREATE TABLE members (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    member_reference NVARCHAR(50) NOT NULL,
    first_name NVARCHAR(100) NOT NULL,
    last_name NVARCHAR(100) NOT NULL,
    membership_type NVARCHAR(50) NOT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_members_member_reference UNIQUE (member_reference)
);

CREATE TABLE bookings (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    confirmation_number NVARCHAR(30) NOT NULL,
    member_id BIGINT NOT NULL,
    destination NVARCHAR(200) NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL DEFAULT 'USD',
    status NVARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_bookings_member FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE booking_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    item_type NVARCHAR(30) NOT NULL,
    supplier NVARCHAR(50) NOT NULL,
    description NVARCHAR(300) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    supplier_confirmation NVARCHAR(60) NULL,
    status NVARCHAR(30) NOT NULL,
    CONSTRAINT fk_booking_items_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE change_requests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    correlation_id NVARCHAR(40) NOT NULL,
    idempotency_key NVARCHAR(100) NOT NULL,
    requested_check_in DATE NOT NULL,
    requested_check_out DATE NOT NULL,
    old_total DECIMAL(10,2) NOT NULL,
    new_total DECIMAL(10,2) NULL,
    price_difference DECIMAL(10,2) NULL,
    status NVARCHAR(30) NOT NULL,
    reconciliation_status NVARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED',
    created_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_change_requests_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT uq_change_requests_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE supplier_reservations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    change_request_id BIGINT NOT NULL,
    supplier_type NVARCHAR(20) NOT NULL,
    supplier_name NVARCHAR(50) NOT NULL,
    supplier_confirmation NVARCHAR(60) NULL,
    status NVARCHAR(30) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    last_checked_at DATETIMEOFFSET(6) NULL,
    CONSTRAINT fk_supplier_reservations_change_request FOREIGN KEY (change_request_id) REFERENCES change_requests(id)
);

CREATE TABLE booking_events (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NULL,
    change_request_id BIGINT NULL,
    correlation_id NVARCHAR(40) NOT NULL,
    event_type NVARCHAR(60) NOT NULL,
    supplier NVARCHAR(50) NULL,
    status NVARCHAR(30) NULL,
    message NVARCHAR(500) NULL,
    latency_ms BIGINT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_booking_events_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_booking_events_change_request FOREIGN KEY (change_request_id) REFERENCES change_requests(id)
);

CREATE TABLE idempotency_records (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    idempotency_key NVARCHAR(100) NOT NULL,
    request_hash NVARCHAR(100) NOT NULL,
    operation NVARCHAR(60) NOT NULL,
    status NVARCHAR(30) NOT NULL,
    response_body VARCHAR(MAX) NULL,
    created_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_idempotency_records_key UNIQUE (idempotency_key)
);

CREATE TABLE incidents (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    correlation_id NVARCHAR(40) NOT NULL,
    external_reference NVARCHAR(60) NOT NULL,
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(1000) NULL,
    status NVARCHAR(30) NOT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSUTCDATETIME()
);
