package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.dto.response.OrderDetailResponse;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderDetailIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    void shouldReturnDetailWithProductSnapshot() {
        // 真实下单：商品 1（机械键盘 399）买 2 件
        OrderResponse placed = orderService.placeOrder(1L, 2);

        OrderDetailResponse detail =
                orderService.getOrderDetail(placed.id());

        assertEquals(placed.id(), detail.id());
        assertEquals(1L, detail.productId());
        assertEquals("机械键盘", detail.productName());
        assertEquals(2, detail.quantity());
        assertEquals(new BigDecimal("798.00"),
                detail.totalAmount());
        assertEquals(OrderStatus.CREATED, detail.status());
    }
}
