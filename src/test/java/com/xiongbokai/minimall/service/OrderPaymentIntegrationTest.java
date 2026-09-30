package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderPaymentIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPayOrderWithoutChangingStock() {
        // 下单：库存 100 → 97
        OrderResponse placedOrder = orderService.placeOrder(1L, 3);

        // 支付
        OrderResponse paidOrder = orderService.pay(placedOrder.id());

        assertEquals(OrderStatus.PAID, paidOrder.status());

        // 支付不动库存，仍是 97
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(97, stock);

        // 数据库里状态确实落库为 PAID
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM purchase_order WHERE id = ?",
                String.class,
                placedOrder.id()
        );
        assertEquals("PAID", status);
    }
}
