package com.xiongbokai.minimall.domain.order;

import java.math.BigDecimal;

public record OrderItemDetail(
        Long id,
        Long productId,
        String productName,
        int quantity,
        BigDecimal price,
        BigDecimal subtotal
) {
}
