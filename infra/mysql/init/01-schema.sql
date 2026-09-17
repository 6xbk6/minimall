CREATE TABLE IF NOT EXISTS product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
       ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT chk_product_name
       CHECK (CHAR_LENGTH(TRIM(name)) > 0),

    CONSTRAINT chk_product_price
       CHECK (price >= 0.01),

    CONSTRAINT chk_product_status
       CHECK (
           status IN (
                      'ON_SALE',
                      'OUT_OF_STOCK',
                      'OFF_SHELF'
               )
           )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;