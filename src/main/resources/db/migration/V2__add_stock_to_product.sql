ALTER TABLE product
    ADD COLUMN stock INT NOT NULL DEFAULT 0 AFTER price,
    ADD CONSTRAINT chk_product_stock
        CHECK (stock >= 0);

UPDATE product
SET stock = CASE
    WHEN status = 'OUT_OF_STOCK' THEN 0
    ELSE 100
END;