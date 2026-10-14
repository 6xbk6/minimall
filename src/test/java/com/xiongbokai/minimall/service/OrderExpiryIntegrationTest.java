package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.dto.request.OrderItemRequest;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderExpiryIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCancelExpiredOrdersAndRestoreStock() {
        // 真实下单：库存 100 → 98，订单 CREATED
        OrderResponse placed = orderService.placeOrder(
                1L,
                List.of(new OrderItemRequest(1L, 2))
        );
        Long orderId = placed.id();

        // 把订单创建时间改到 1 小时前，模拟"下单后超时未支付"
        jdbcTemplate.update(
                "UPDATE purchase_order " +
                        "SET created_at = DATEADD('MINUTE', -60, CURRENT_TIMESTAMP) " +
                        "WHERE id = ?",
                orderId
        );

        int closed = orderService.cancelExpiredOrders(30);

        assertEquals(1, closed);

        // 订单状态真实落库为 CANCELLED
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM purchase_order WHERE id = ?",
                String.class,
                orderId
        );
        assertEquals("CANCELLED", status);

        // 库存还回 100
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(100, stock);
    }

    @Test
    void shouldKeepFreshOrdersUntouched() {
        // 刚下的单（创建时间就是现在）：不超时，不应被关
        OrderResponse placed = orderService.placeOrder(
                1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        int closed = orderService.cancelExpiredOrders(30);

        assertEquals(0, closed);

        // 订单仍是 CREATED
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM purchase_order WHERE id = ?",
                String.class,
                placed.id()
        );
        assertEquals("CREATED", status);

        // 库存仍扣着 98，没有被错误还回
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(98, stock);
    }
}
