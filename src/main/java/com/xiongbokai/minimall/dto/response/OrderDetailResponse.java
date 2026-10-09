package com.xiongbokai.minimall.dto.response;

import com.xiongbokai.minimall.domain.order.OrderStatus;

import java.math.BigDecimal;

public record OrderDetailResponse(
        Long id,
        Long productId,
        String productName,
        int quantity,
        BigDecimal totalAmount,
        OrderStatus status
) {
}
