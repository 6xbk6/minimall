package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.exception.IllegalOrderStatusException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderRefundIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRestoreStockAndRefundPaidOrderTogether() {
        // 下单 + 支付：库存 100 → 97
        OrderResponse placed = orderService.placeOrder(1L, 3);
        orderService.pay(placed.id());

        // 退款
        OrderResponse refunded =
                orderService.refund(placed.id());

        assertEquals(OrderStatus.REFUNDED, refunded.status());

        // 库存还回 100
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(100, stock);

        // 状态真实落库
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM purchase_order WHERE id = ?",
                String.class,
                placed.id()
        );
        assertEquals("REFUNDED", status);
    }

    @Test
    void shouldNotRefundCreatedOrderAndKeepStockUnchanged() {
        // 只下单不支付：库存 100 → 98，状态 CREATED
        OrderResponse placed = orderService.placeOrder(1L, 2);

        assertThrows(
                IllegalOrderStatusException.class,
                () -> orderService.refund(placed.id())
        );

        // 库存仍是 98，没有被错误还回
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(98, stock);
    }
}
