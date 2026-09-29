package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.OrderStatus;
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
        OrderResponse response = orderService.placeOrder(1L, 3);

        assertEquals(1L, response.productId());
        assertEquals(3, response.quantity());
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
    }

    @Test
    void shouldLeaveNoOrderWhenStockIsInsufficient() {
        assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder(2L, 1)
        );

        assertEquals(0, orderRepository.findAll().size());

        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 2",
                Integer.class
        );
        assertEquals(0, stock);
    }
}
