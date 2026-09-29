CREATE TABLE purchase_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT chk_order_quantity
        CHECK (quantity >= 1),

    CONSTRAINT chk_order_total
        CHECK (total_amount >= 0),

    CONSTRAINT chk_order_status
        CHECK (status IN ('CREATED', 'PAID', 'CANCELLED')),

    CONSTRAINT fk_order_product
        FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
