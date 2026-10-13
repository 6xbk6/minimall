package com.xiongbokai.minimall.domain.order;

import java.math.BigDecimal;

public record Order(
        Long id,
        Long buyerId,
        BigDecimal totalAmount,
        OrderStatus status
) {
}
