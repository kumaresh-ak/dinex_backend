CREATE TABLE roles (
    code VARCHAR(30) PRIMARY KEY,
    description VARCHAR(200) NOT NULL
);

INSERT INTO roles (code, description) VALUES
('SUPER_ADMIN', 'Platform owner who manages hotels and subscriptions'),
('HOTEL_ADMIN', 'Hotel owner who manages menu, tables, orders and billing'),
('CHEF', 'Kitchen staff who updates order preparation status'),
('CUSTOMER', 'Guest who orders through the table QR code');

ALTER TABLE users ADD CONSTRAINT fk_users_role FOREIGN KEY (role) REFERENCES roles(code);
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP NULL;

ALTER TABLE hotels ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE menu_items ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE dining_tables ADD COLUMN qr_token VARCHAR(64) UNIQUE;
ALTER TABLE dining_tables ADD COLUMN capacity INT NOT NULL DEFAULT 4;

CREATE TABLE customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    customer_name VARCHAR(160) NOT NULL,
    mobile_number VARCHAR(30) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    UNIQUE KEY uk_customer_tenant_mobile (hotel_id, mobile_number)
);

ALTER TABLE dining_sessions ADD COLUMN session_token VARCHAR(64) UNIQUE;
ALTER TABLE dining_sessions ADD COLUMN customer_id BIGINT NULL;
ALTER TABLE dining_sessions ADD CONSTRAINT fk_session_customer FOREIGN KEY (customer_id) REFERENCES customers(id);

CREATE TABLE bills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL UNIQUE,
    bill_number VARCHAR(40) NOT NULL UNIQUE,
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    sgst DECIMAL(12,2) NOT NULL DEFAULT 0,
    cgst DECIMAL(12,2) NOT NULL DEFAULT 0,
    tip_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL,
    CONSTRAINT fk_bill_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    CONSTRAINT fk_bill_session FOREIGN KEY (session_id) REFERENCES dining_sessions(id),
    INDEX idx_bill_tenant_status (hotel_id, status, paid_at)
);

ALTER TABLE payments ADD COLUMN bill_id BIGINT NULL;
ALTER TABLE payments ADD CONSTRAINT fk_payment_bill FOREIGN KEY (bill_id) REFERENCES bills(id);

CREATE TABLE feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL UNIQUE,
    rating INT NOT NULL,
    comments VARCHAR(1000),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    CONSTRAINT fk_feedback_session FOREIGN KEY (session_id) REFERENCES dining_sessions(id),
    INDEX idx_feedback_tenant (hotel_id, created_date)
);

CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    INDEX idx_notification_tenant (hotel_id, read_flag, created_date)
);

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NULL,
    username VARCHAR(190) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80),
    details VARCHAR(1000),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_tenant (hotel_id, created_date)
);

CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_refresh_user (user_id, revoked)
);

CREATE TABLE password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE platform_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(500) NOT NULL,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
