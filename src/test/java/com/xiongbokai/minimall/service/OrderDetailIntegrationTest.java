package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.dto.request.OrderItemRequest;
import com.xiongbokai.minimall.dto.response.OrderDetailResponse;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderDetailIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    void shouldReturnDetailWithItemsSnapshot() {
        // 真实下单：商品 1（机械键盘 399）买 2 件
        OrderResponse placed = orderService.placeOrder(
                1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        OrderDetailResponse detail =
                orderService.getOrderDetail(placed.id());

        assertEquals(placed.id(), detail.id());
        assertEquals(1L, detail.buyerId());
        assertEquals("demo_user", detail.buyerName());
        assertEquals(new BigDecimal("798.00"),
                detail.totalAmount());
        assertEquals(OrderStatus.CREATED, detail.status());

        // 明细快照：商品名、数量、单价、小计
        assertEquals(1, detail.items().size());
        assertEquals("机械键盘", detail.items().get(0).productName());
        assertEquals(2, detail.items().get(0).quantity());
        assertEquals(new BigDecimal("399.00"),
                detail.items().get(0).price());
        assertEquals(new BigDecimal("798.00"),
                detail.items().get(0).subtotal());
    }

    @Test
    void shouldReturnDetailWithMultipleItems() {
        // 一单两件：键盘 399×1 + 支架 259×1 = 658
        OrderResponse placed = orderService.placeOrder(
                1L,
                List.of(
                        new OrderItemRequest(1L, 1),
                        new OrderItemRequest(3L, 1)
                )
        );

        OrderDetailResponse detail =
                orderService.getOrderDetail(placed.id());

        assertEquals(new BigDecimal("658.00"),
                detail.totalAmount());
        assertEquals(2, detail.items().size());
        assertEquals("机械键盘",
                detail.items().get(0).productName());
        assertEquals("显示器支架",
                detail.items().get(1).productName());
    }
}
