-- 1. 建订单明细表
CREATE TABLE order_item
(
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    order_id   BIGINT         NOT NULL,
    product_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL,
    price      DECIMAL(12, 2) NOT NULL,
    subtotal   DECIMAL(12, 2) NOT NULL,
    created_at DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT chk_item_quantity
        CHECK (quantity >= 1),

    CONSTRAINT chk_item_price
        CHECK (price >= 0.01),

    CONSTRAINT chk_item_subtotal
        CHECK (subtotal >= 0),

    CONSTRAINT fk_item_order
        FOREIGN KEY (order_id) REFERENCES purchase_order (id),

    CONSTRAINT fk_item_product
        FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 2. 存量订单迁到明细表（单价 = 总额 / 数量）
INSERT INTO order_item (order_id, product_id, quantity, price, subtotal)
SELECT id,
       product_id,
       quantity,
       total_amount / quantity,
       total_amount
FROM purchase_order;

-- 3. 订单表去掉商品相关列（订单从此只关心"谁买的总共多少"）
ALTER TABLE purchase_order
    DROP COLUMN product_id;
ALTER TABLE purchase_order
    DROP COLUMN quantity;
