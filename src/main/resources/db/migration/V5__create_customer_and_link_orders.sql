CREATE TABLE customer
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT chk_customer_username
        CHECK (CHAR_LENGTH(TRIM(username)) > 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO customer (id, username)
VALUES (1, 'demo_user');

ALTER TABLE purchase_order
    ADD COLUMN buyer_id BIGINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT fk_order_buyer
        FOREIGN KEY (buyer_id) REFERENCES customer (id);
