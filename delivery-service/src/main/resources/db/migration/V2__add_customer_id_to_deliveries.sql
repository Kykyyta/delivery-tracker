ALTER TABLE deliveries
    ADD COLUMN customer_id BIGINT;

CREATE INDEX idx_deliveries_customer_id
    ON deliveries(customer_id);