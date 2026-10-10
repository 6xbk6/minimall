package com.xiongbokai.minimall.domain.order;

import java.math.BigDecimal;

public record OrderDetail(
        Long id,
        Long productId,
        String productName,
        int quantity,
        BigDecimal totalAmount,
        OrderStatus status
) {
}
