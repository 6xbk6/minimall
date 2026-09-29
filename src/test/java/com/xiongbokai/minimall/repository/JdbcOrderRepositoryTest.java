package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcOrderRepositoryTest {

    private final JdbcOrderRepository repository;

    @Autowired
    JdbcOrderRepositoryTest(JdbcOrderRepository repository) {
        this.repository = repository;
    }

    @Test
    void shouldInsertOrderAndGenerateId() {
        Order order = new Order(
                null,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CREATED
        );

        Order savedOrder = repository.save(order);

        assertTrue(savedOrder.id() != null);
        assertEquals(1L, savedOrder.productId());
        assertEquals(2, savedOrder.quantity());
        assertEquals(
                new BigDecimal("798.00"),
                savedOrder.totalAmount()
        );
        assertEquals(OrderStatus.CREATED, savedOrder.status());

        assertEquals(
                savedOrder,
                repository.findById(savedOrder.id()).orElseThrow()
        );
    }

    @Test
    void shouldFindOrderById() {
        Order savedOrder = repository.save(new Order(
                null,
                2L,
                1,
                new BigDecimal("129.00"),
                OrderStatus.CREATED
        ));

        Order order = repository.findById(savedOrder.id()).orElseThrow();

        assertEquals(savedOrder, order);
    }

    @Test
    void shouldFindAllOrders() {
        repository.save(new Order(
                null,
                1L,
                1,
                new BigDecimal("399.00"),
                OrderStatus.CREATED
        ));
        repository.save(new Order(
                null,
                3L,
                1,
                new BigDecimal("259.00"),
                OrderStatus.CREATED
        ));

        List<Order> orders = repository.findAll();

        assertEquals(2, orders.size());
    }
}
