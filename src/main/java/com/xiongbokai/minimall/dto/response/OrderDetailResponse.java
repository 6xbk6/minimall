//package com.xiongbokai.minimall.dto.response;
//
//import com.xiongbokai.minimall.domain.order.OrderStatus;
//
//import java.math.BigDecimal;
//
//public record OrderDetailResponse(
//        Long id,
//        Long productId,
//        String productName,
//        int quantity,
//        BigDecimal totalAmount,
//        OrderStatus status,
//        Long buyerId,
//        String buyerName
//) {
//}

package com.xiongbokai.minimall.dto.response;

import com.xiongbokai.minimall.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        Long buyerId,
        String buyerName,
        BigDecimal totalAmount,
        OrderStatus status,
        List<OrderItemResponse> items
) {
}
