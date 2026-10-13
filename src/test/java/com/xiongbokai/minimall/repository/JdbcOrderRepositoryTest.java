package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderDetail;
import com.xiongbokai.minimall.domain.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
                OrderStatus.CREATED,
                1L
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
                OrderStatus.CREATED,
                1L
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
                OrderStatus.CREATED,
                1L
        ));
        repository.save(new Order(
                null,
                3L,
                1,
                new BigDecimal("259.00"),
                OrderStatus.CREATED,
                1L
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
                OrderStatus.CREATED,
                1L
        );

        Order secondOrder = new Order(
                null,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CREATED,
                1L
        );

        repository.save(firstOrder);
        repository.save(secondOrder);

        long count = repository.count(null, null);

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
                    OrderStatus.CREATED,
                    1L
            ));
        }

        // 第一页：取 3 条
        List<Order> firstPage = repository.findPage(null,null,0, 3);

        assertEquals(3, firstPage.size());
        assertEquals(1, firstPage.get(0).quantity());
        assertEquals(3, firstPage.get(2).quantity());

        // 第二页：跳过 3 条，只剩 2 条
        List<Order> secondPage = repository.findPage(null, null,3, 3);

        assertEquals(2, secondPage.size());
        assertEquals(4, secondPage.get(0).quantity());
        assertEquals(5, secondPage.get(1).quantity());
    }

    @Test
    void shouldCountOrdersByStatus() {
        repository.save(new Order(
                null, 1L, 1,
                new BigDecimal("399.00"),
                OrderStatus.CREATED,
                1L
        ));
        repository.save(new Order(
                null, 1L, 2,
                new BigDecimal("798.00"),
                OrderStatus.PAID,
                1L
        ));

        assertEquals(1L, repository.count(OrderStatus.CREATED, null));
        assertEquals(1L, repository.count(OrderStatus.PAID, null));
        assertEquals(0L, repository.count(OrderStatus.CANCELLED, null));
    }

    @Test
    void shouldReturnPagedOrdersByStatus() {
        for (int i = 1; i <= 3; i++) {
            repository.save(new Order(
                    null, 1L, i,
                    new BigDecimal("399.00"),
                    OrderStatus.CREATED,
                    1L
            ));
        }
        repository.save(new Order(
                null, 1L, 9,
                new BigDecimal("399.00"),
                OrderStatus.PAID,
                1L
        ));

        List<Order> paidOrders =
                repository.findPage(OrderStatus.PAID, null, 0, 10);

        assertEquals(1, paidOrders.size());
        assertEquals(OrderStatus.PAID, paidOrders.get(0).status());

        List<Order> createdPage =
                repository.findPage(OrderStatus.CREATED, null, 0, 2);

        assertEquals(2, createdPage.size());
    }

    @Test
    void shouldFindOrderDetailWithProductNameByJoin() {
        Order order = repository.save(new Order(
                null,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CREATED,
                1L
        ));

        Optional<OrderDetail> detail =
                repository.findDetailById(order.id());

        assertTrue(detail.isPresent());
        assertEquals("机械键盘", detail.get().productName());
        assertEquals(2, detail.get().quantity());
        assertEquals(new BigDecimal("798.00"),
                detail.get().totalAmount());
        assertEquals(OrderStatus.CREATED, detail.get().status());
        assertEquals(1L, detail.get().buyerId());
        assertEquals("demo_user", detail.get().buyerName());

    }

    @Test
    void shouldReturnEmptyWhenOrderDetailNotFound() {
        Optional<OrderDetail> detail =
                repository.findDetailById(999L);

        assertTrue(detail.isEmpty());
    }

    @Test
    void shouldFilterOrdersByBuyer() {
        repository.save(new Order(
                null, 1L, 1,
                new BigDecimal("399.00"),
                OrderStatus.CREATED,
                1L
        ));
        repository.save(new Order(
                null, 1L, 1,
                new BigDecimal("399.00"),
                OrderStatus.CREATED,
                2L
        ));

        long buyer1Count = repository.count(null, 1L);
        assertEquals(1L, buyer1Count);

        List<Order> buyer1Orders =
                repository.findPage(null, 1L, 0, 10);
        assertEquals(1, buyer1Orders.size());
        assertEquals(1L, buyer1Orders.get(0).buyerId());
    }

    @Test
    void shouldFilterOrdersByStatusAndBuyerTogether() {
        repository.save(new Order(
                null, 1L, 1,
                new BigDecimal("399.00"),
                OrderStatus.CREATED,
                1L
        ));
        repository.save(new Order(
                null, 1L, 1,
                new BigDecimal("399.00"),
                OrderStatus.PAID,
                1L
        ));
        repository.save(new Order(
                null, 1L, 1,
                new BigDecimal("399.00"),
                OrderStatus.PAID,
                2L
        ));

        // 组合：买家 1 的 PAID 订单 → 只有 1 笔
        long combinedCount =
                repository.count(OrderStatus.PAID, 1L);
        assertEquals(1L, combinedCount);

        List<Order> combined =
                repository.findPage(
                        OrderStatus.PAID, 1L, 0, 10
                );
        assertEquals(1, combined.size());
    }

}
