package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderDetail;
import com.xiongbokai.minimall.domain.order.OrderItem;
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

    private Order orderWith(BigDecimal total, OrderStatus status) {
        return new Order(null, 1L, total, status);
    }

    private OrderItem itemOf(Long productId, int quantity, BigDecimal price) {
        return new OrderItem(
                null,
                null,
                productId,
                quantity,
                price,
                price.multiply(BigDecimal.valueOf(quantity))
        );
    }

    @Test
    void shouldInsertOrderAndGenerateId() {
        Order order = orderWith(
                new BigDecimal("798.00"),
                OrderStatus.CREATED
        );
        OrderItem item = itemOf(1L, 2, new BigDecimal("399.00"));

        Order savedOrder = repository.save(order, List.of(item));

        assertTrue(savedOrder.id() != null);
        assertEquals(1L, savedOrder.buyerId());
        assertEquals(
                new BigDecimal("798.00"),
                savedOrder.totalAmount()
        );
        assertEquals(OrderStatus.CREATED, savedOrder.status());

        assertEquals(
                savedOrder,
                repository.findById(savedOrder.id()).orElseThrow()
        );

        // 明细真实落库，且挂在订单 id 下
        List<OrderItem> items =
                repository.findItemsByOrderId(savedOrder.id());

        assertEquals(1, items.size());
        assertEquals(1L, items.get(0).productId());
        assertEquals(2, items.get(0).quantity());
        assertEquals(
                new BigDecimal("399.00"),
                items.get(0).price()
        );
        assertEquals(
                new BigDecimal("798.00"),
                items.get(0).subtotal()
        );
    }

    @Test
    void shouldSaveMultipleItemsUnderOneOrder() {
        Order order = orderWith(
                new BigDecimal("658.00"),
                OrderStatus.CREATED
        );
        OrderItem keyboard = itemOf(1L, 1, new BigDecimal("399.00"));
        OrderItem stand = itemOf(3L, 1, new BigDecimal("259.00"));

        Order savedOrder = repository.save(
                order,
                List.of(keyboard, stand)
        );

        List<OrderItem> items =
                repository.findItemsByOrderId(savedOrder.id());

        // 一单两件：键盘 + 支架
        assertEquals(2, items.size());
        assertEquals(1L, items.get(0).productId());
        assertEquals(3L, items.get(1).productId());
    }

    @Test
    void shouldFindOrderById() {
        Order order = orderWith(
                new BigDecimal("129.00"),
                OrderStatus.CREATED
        );
        OrderItem item = itemOf(2L, 1, new BigDecimal("129.00"));

        Order savedOrder = repository.save(order, List.of(item));

        Order found = repository.findById(savedOrder.id()).orElseThrow();

        assertEquals(savedOrder, found);
    }

    @Test
    void shouldFindAllOrders() {
        repository.save(
                orderWith(new BigDecimal("399.00"), OrderStatus.CREATED),
                List.of(itemOf(1L, 1, new BigDecimal("399.00")))
        );
        repository.save(
                orderWith(new BigDecimal("259.00"), OrderStatus.CREATED),
                List.of(itemOf(3L, 1, new BigDecimal("259.00")))
        );

        List<Order> orders = repository.findAll();

        assertEquals(2, orders.size());
    }

    @Test
    void shouldCountOrders() {
        repository.save(
                orderWith(new BigDecimal("399.00"), OrderStatus.CREATED),
                List.of(itemOf(1L, 1, new BigDecimal("399.00")))
        );
        repository.save(
                orderWith(new BigDecimal("798.00"), OrderStatus.CREATED),
                List.of(itemOf(1L, 2, new BigDecimal("399.00")))
        );

        long count = repository.count(null, null);

        assertEquals(2L, count);
    }

    @Test
    void shouldReturnPagedOrdersOrderedById() {
        for (int i = 1; i <= 5; i++) {
            repository.save(
                    orderWith(
                            new BigDecimal(String.valueOf(399L * i)),
                            OrderStatus.CREATED
                    ),
                    List.of(itemOf(1L, i, new BigDecimal("399.00")))
            );
        }

        List<Order> firstPage = repository.findPage(null, null, 0, 3);

        assertEquals(3, firstPage.size());
        assertEquals(
                new BigDecimal("399.00"),
                firstPage.get(0).totalAmount()
        );
        assertEquals(
                new BigDecimal("1197.00"),
                firstPage.get(2).totalAmount()
        );

        List<Order> secondPage = repository.findPage(null, null, 3, 3);

        assertEquals(2, secondPage.size());
        assertEquals(
                new BigDecimal("1596.00"),
                secondPage.get(0).totalAmount()
        );
        assertEquals(
                new BigDecimal("1995.00"),
                secondPage.get(1).totalAmount()
        );
    }

    @Test
    void shouldCountOrdersByStatus() {
        repository.save(
                orderWith(new BigDecimal("399.00"), OrderStatus.CREATED),
                List.of(itemOf(1L, 1, new BigDecimal("399.00")))
        );
        repository.save(
                orderWith(new BigDecimal("798.00"), OrderStatus.PAID),
                List.of(itemOf(1L, 2, new BigDecimal("399.00")))
        );

        assertEquals(1L, repository.count(OrderStatus.CREATED, null));
        assertEquals(1L, repository.count(OrderStatus.PAID, null));
        assertEquals(0L, repository.count(OrderStatus.CANCELLED, null));
    }

    @Test
    void shouldReturnPagedOrdersByStatus() {
        for (int i = 1; i <= 3; i++) {
            repository.save(
                    orderWith(
                            new BigDecimal("399.00"),
                            OrderStatus.CREATED
                    ),
                    List.of(itemOf(1L, i, new BigDecimal("399.00")))
            );
        }
        repository.save(
                orderWith(new BigDecimal("399.00"), OrderStatus.PAID),
                List.of(itemOf(1L, 9, new BigDecimal("399.00")))
        );

        List<Order> paidOrders =
                repository.findPage(OrderStatus.PAID, null, 0, 10);

        assertEquals(1, paidOrders.size());
        assertEquals(OrderStatus.PAID, paidOrders.get(0).status());

        List<Order> createdPage =
                repository.findPage(OrderStatus.CREATED, null, 0, 2);

        assertEquals(2, createdPage.size());
    }

    @Test
    void shouldFindOrderDetailWithItems() {
        Order order = orderWith(
                new BigDecimal("798.00"),
                OrderStatus.CREATED
        );
        OrderItem item = itemOf(1L, 2, new BigDecimal("399.00"));

        Order savedOrder = repository.save(order, List.of(item));

        Optional<OrderDetail> detail =
                repository.findDetailById(savedOrder.id());

        assertTrue(detail.isPresent());
        assertEquals("demo_user", detail.get().buyerName());
        assertEquals(1L, detail.get().buyerId());
        assertEquals(
                new BigDecimal("798.00"),
                detail.get().totalAmount()
        );
        assertEquals(OrderStatus.CREATED, detail.get().status());

        // 明细带商品名
        assertEquals(1, detail.get().items().size());
        assertEquals("机械键盘", detail.get().items().get(0).productName());
        assertEquals(2, detail.get().items().get(0).quantity());
    }

    @Test
    void shouldReturnEmptyWhenOrderDetailNotFound() {
        Optional<OrderDetail> detail =
                repository.findDetailById(999L);

        assertTrue(detail.isEmpty());
    }

    @Test
    void shouldFilterOrdersByBuyer() {
        repository.save(
                orderWith(new BigDecimal("399.00"), OrderStatus.CREATED),
                List.of(itemOf(1L, 1, new BigDecimal("399.00")))
        );
        repository.save(
                new Order(
                        null,
                        2L,
                        new BigDecimal("399.00"),
                        OrderStatus.CREATED
                ),
                List.of(itemOf(1L, 1, new BigDecimal("399.00")))
        );

        long buyer1Count = repository.count(null, 1L);
        assertEquals(1L, buyer1Count);

        List<Order> buyer1Orders =
                repository.findPage(null, 1L, 0, 10);
        assertEquals(1, buyer1Orders.size());
        assertEquals(1L, buyer1Orders.get(0).buyerId());
    }

    @Test
    void shouldFilterOrdersByStatusAndBuyerTogether() {
        repository.save(
                orderWith(new BigDecimal("399.00"), OrderStatus.CREATED),
                List.of(itemOf(1L, 1, new BigDecimal("399.00")))
        );
        repository.save(
                orderWith(new BigDecimal("399.00"), OrderStatus.PAID),
                List.of(itemOf(1L, 1, new BigDecimal("399.00")))
        );
        repository.save(
                new Order(
                        null,
                        2L,
                        new BigDecimal("399.00"),
                        OrderStatus.PAID
                ),
                List.of(itemOf(1L, 1, new BigDecimal("399.00")))
        );

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
