//package com.xiongbokai.minimall.domain.order;
//
//import java.math.BigDecimal;
//
//public record OrderDetail(
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

package com.xiongbokai.minimall.domain.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetail(
        Long id,
        Long buyerId,
        String buyerName,
        BigDecimal totalAmount,
        OrderStatus status,
        List<OrderItemDetail> items
) {
}

