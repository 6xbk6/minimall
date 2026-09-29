package com.xiongbokai.minimall.dto.response;

import com.xiongbokai.minimall.domain.order.OrderStatus;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        Long productId,
        int quantity,
        BigDecimal totalAmount,
        OrderStatus status
) {
}
