package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.exception.IllegalOrderStatusException;
import com.xiongbokai.minimall.repository.OrderRepository;
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
class OrderCancellationIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRestoreStockAndCancelOrderTogether() {
        // 先下单：库存 100 → 97，产生 1 笔订单
        OrderResponse placedOrder = orderService.placeOrder(1L, 3);

        Integer stockBeforeCancel = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(97, stockBeforeCancel);

        // 取消
        OrderResponse cancelledOrder =
                orderService.cancel(placedOrder.id());

        assertEquals(
                OrderStatus.CANCELLED,
                cancelledOrder.status()
        );

        Integer finalStock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(100, finalStock);
    }

    @Test
    void shouldNotCancelPaidOrderAndKeepStockUnchanged() {
        OrderResponse placedOrder = orderService.placeOrder(1L, 2);

        // 直接把订单改成 PAID，模拟已支付
        orderRepository.updateStatus(
                placedOrder.id(),
                OrderStatus.PAID
        );

        assertThrows(
                IllegalOrderStatusException.class,
                () -> orderService.cancel(placedOrder.id())
        );

        // 库存仍是扣减后的 98，没有被错误还回
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(98, stock);
    }
}
