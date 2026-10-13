package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.dto.request.OrderItemRequest;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.exception.InsufficientStockException;
import com.xiongbokai.minimall.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlaceOrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateOrderAndDeductStockTogether() {
        OrderResponse response = orderService.placeOrder(
                1L,
                List.of(new OrderItemRequest(1L, 3))
        );

        assertEquals(1L, response.buyerId());
        assertEquals(
                new BigDecimal("1197.00"),
                response.totalAmount()
        );
        assertEquals(OrderStatus.CREATED, response.status());

        assertEquals(1, orderRepository.findAll().size());

        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(97, stock);

        // 明细真实落库
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_item WHERE order_id = ?",
                Integer.class,
                response.id()
        );
        assertEquals(1, itemCount);
    }

    @Test
    void shouldCreateOrderWithMultipleItems() {
        // 键盘 399×2 + 支架 259×1 = 1057
        OrderResponse response = orderService.placeOrder(
                1L,
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(3L, 1)
                )
        );

        assertEquals(
                new BigDecimal("1057.00"),
                response.totalAmount()
        );
        assertEquals(OrderStatus.CREATED, response.status());

        // 两件商品库存都被扣
        Integer keyboardStock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );
        assertEquals(98, keyboardStock);

        Integer standStock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 3",
                Integer.class
        );
        assertEquals(99, standStock);

        // 一单两条明细
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_item WHERE order_id = ?",
                Integer.class,
                response.id()
        );
        assertEquals(2, itemCount);
    }

    @Test
    void shouldLeaveNoOrderWhenStockIsInsufficient() {
        assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder(
                        1L,
                        List.of(new OrderItemRequest(2L, 1))
                )
        );

        assertEquals(0, orderRepository.findAll().size());

        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 2",
                Integer.class
        );
        assertEquals(0, stock);
    }
}
