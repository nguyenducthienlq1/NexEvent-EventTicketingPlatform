ALTER TABLE ticket_types
    ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE orders
    ADD CONSTRAINT check_order_status
        CHECK (status IN ('PENDING', 'PAID', 'CANCELLED'));