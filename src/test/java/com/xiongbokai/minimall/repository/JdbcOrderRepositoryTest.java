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

    @Test
    void shouldCountOrders() {
        Order firstOrder = new Order(
                null,
                1L,
                1,
                new BigDecimal("399.00"),
                OrderStatus.CREATED
        );

        Order secondOrder = new Order(
                null,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CREATED
        );

        repository.save(firstOrder);
        repository.save(secondOrder);

        long count = repository.count();

        assertEquals(2L, count);
    }

    @Test
    void shouldReturnPagedOrdersOrderedById() {
        for (int i = 1; i <= 5; i++) {
            repository.save(new Order(
                    null,
                    1L,
                    i,
                    new BigDecimal(String.valueOf(399L * i)),
                    OrderStatus.CREATED
            ));
        }

        // 第一页：取 3 条
        List<Order> firstPage = repository.findPage(0, 3);

        assertEquals(3, firstPage.size());
        assertEquals(1, firstPage.get(0).quantity());
        assertEquals(3, firstPage.get(2).quantity());

        // 第二页：跳过 3 条，只剩 2 条
        List<Order> secondPage = repository.findPage(3, 3);

        assertEquals(2, secondPage.size());
        assertEquals(4, secondPage.get(0).quantity());
        assertEquals(5, secondPage.get(1).quantity());
    }

    @Test
    void shouldCountOrdersByStatus() {
        repository.save(new Order(
                null, 1L, 1,
                new BigDecimal("399.00"),
                OrderStatus.CREATED
        ));
        repository.save(new Order(
                null, 1L, 2,
                new BigDecimal("798.00"),
                OrderStatus.PAID
        ));

        assertEquals(1L, repository.countByStatus(OrderStatus.CREATED));
        assertEquals(1L, repository.countByStatus(OrderStatus.PAID));
        assertEquals(0L, repository.countByStatus(OrderStatus.CANCELLED));
    }

    @Test
    void shouldReturnPagedOrdersByStatus() {
        for (int i = 1; i <= 3; i++) {
            repository.save(new Order(
                    null, 1L, i,
                    new BigDecimal("399.00"),
                    OrderStatus.CREATED
            ));
        }
        repository.save(new Order(
                null, 1L, 9,
                new BigDecimal("399.00"),
                OrderStatus.PAID
        ));

        List<Order> paidOrders =
                repository.findPageByStatus(OrderStatus.PAID, 0, 10);

        assertEquals(1, paidOrders.size());
        assertEquals(OrderStatus.PAID, paidOrders.get(0).status());

        List<Order> createdPage =
                repository.findPageByStatus(OrderStatus.CREATED, 0, 2);

        assertEquals(2, createdPage.size());
    }

}
