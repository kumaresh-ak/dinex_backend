CREATE TABLE hotels (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_name VARCHAR(160) NOT NULL,
    owner_name VARCHAR(160) NOT NULL,
    mobile_number VARCHAR(30) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    gst_number VARCHAR(30),
    upi_id VARCHAR(120) NOT NULL,
    logo_url VARCHAR(500),
    qr_image_url VARCHAR(500),
    subscription_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_hotels_status (subscription_status, deleted_flag)
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT,
    username VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    mobile_number VARCHAR(30),
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    INDEX idx_users_tenant (hotel_id, role, enabled)
);

CREATE TABLE subscription_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(30) NOT NULL UNIQUE,
    plan_name VARCHAR(80) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    duration_months INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE hotel_subscriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    payment_reference VARCHAR(120),
    payment_date DATE,
    payment_proof_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
    start_date DATE,
    end_date DATE,
    approved_by BIGINT,
    approved_date TIMESTAMP NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscription_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
    INDEX idx_subscription_tenant (hotel_id, status, end_date)
);

CREATE TABLE menu_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_priority INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    UNIQUE KEY uk_category_tenant_name (hotel_id, name),
    INDEX idx_category_tenant (hotel_id, enabled, deleted_flag)
);

CREATE TABLE menu_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    price DECIMAL(12,2) NOT NULL,
    image_url VARCHAR(500),
    preparation_minutes INT NOT NULL DEFAULT 15,
    food_type VARCHAR(20) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    display_priority INT NOT NULL DEFAULT 0,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES menu_categories(id),
    INDEX idx_items_tenant_visibility (hotel_id, available, deleted_flag)
);

CREATE TABLE dining_tables (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    table_number VARCHAR(30) NOT NULL,
    table_name VARCHAR(100),
    qr_url VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_table_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    UNIQUE KEY uk_table_tenant_number (hotel_id, table_number)
);

CREATE TABLE dining_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    session_number VARCHAR(40) NOT NULL UNIQUE,
    customer_name VARCHAR(160),
    customer_mobile VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    CONSTRAINT fk_session_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    CONSTRAINT fk_session_table FOREIGN KEY (table_id) REFERENCES dining_tables(id),
    INDEX idx_session_tenant_status (hotel_id, status, table_id)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    order_number VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    sgst DECIMAL(12,2) NOT NULL DEFAULT 0,
    cgst DECIMAL(12,2) NOT NULL DEFAULT 0,
    tip_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    notes VARCHAR(500),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    CONSTRAINT fk_order_session FOREIGN KEY (session_id) REFERENCES dining_sessions(id),
    INDEX idx_order_tenant_status (hotel_id, status, created_date)
);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    item_name VARCHAR(160) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    special_instructions VARCHAR(500),
    CONSTRAINT fk_order_item_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_item_menu FOREIGN KEY (menu_item_id) REFERENCES menu_items(id),
    INDEX idx_order_items_tenant (hotel_id, order_id)
);

CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    tip_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP NULL,
    verified_by BIGINT,
    CONSTRAINT fk_payment_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
    CONSTRAINT fk_payment_session FOREIGN KEY (session_id) REFERENCES dining_sessions(id),
    INDEX idx_payments_tenant_status (hotel_id, status, paid_at)
);

INSERT INTO subscription_plans (plan_code, plan_name, price, duration_months) VALUES
('STARTER', 'Starter', 1999.00, 1),
('STANDARD', 'Standard', 4999.00, 3),
('PRO', 'Pro', 9999.00, 6);
