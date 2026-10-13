package com.xiongbokai.minimall.domain.order;

import java.math.BigDecimal;

public record OrderItem(
        Long id,
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal price,
        BigDecimal subtotal
) {
}
