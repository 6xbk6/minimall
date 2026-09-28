package com.xiongbokai.minimall.domain.product;

import java.math.BigDecimal;

public record Product(
        Long id,
        String name,
        BigDecimal price,
        int stock,
        ProductStatus status
) {
}
