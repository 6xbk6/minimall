ALTER TABLE purchase_order
    DROP CONSTRAINT chk_order_status;

ALTER TABLE purchase_order
    ADD CONSTRAINT chk_order_status
        CHECK (
            status IN (
                       'CREATED',
                       'PAID',
                       'CANCELLED',
                       'REFUNDED'
                )
            );
