ALTER TABLE deliveries
    ADD COLUMN courier_user_id BIGINT;

CREATE INDEX idx_deliveries_courier_user_id
    ON deliveries(courier_user_id);