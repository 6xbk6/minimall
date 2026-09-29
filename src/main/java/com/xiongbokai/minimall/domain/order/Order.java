package com.xiongbokai.minimall.domain.order;

import java.math.BigDecimal;

public record Order(
        Long id,
        Long productId,
        int quantity,
        BigDecimal totalAmount,
        OrderStatus status
) {
}
