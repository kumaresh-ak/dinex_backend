ALTER TABLE hotels ADD COLUMN menu_qr_token VARCHAR(64) NULL UNIQUE;

CREATE TABLE support_tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    subject VARCHAR(160) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    resolution VARCHAR(2000) NULL,
    created_by VARCHAR(190) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_date TIMESTAMP NULL,
    INDEX idx_support_hotel (hotel_id, created_date),
    INDEX idx_support_status (status, created_date)
);
