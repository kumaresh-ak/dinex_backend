ALTER TABLE payments ADD COLUMN created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE payments ADD INDEX idx_payments_bill (bill_id, status);
