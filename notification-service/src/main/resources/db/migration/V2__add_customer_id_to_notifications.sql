ALTER TABLE notifications
    ADD COLUMN customer_id BIGINT;

CREATE INDEX idx_notifications_customer_id
    ON notifications(customer_id);