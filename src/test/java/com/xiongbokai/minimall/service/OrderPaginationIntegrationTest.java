package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.dto.response.PageResponse;
import com.xiongbokai.minimall.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderPaginationIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldSplitOrdersIntoPages() {
        for (int i = 1; i <= 15; i++) {
            orderRepository.save(new Order(
                    null,
                    1L,
                    1,
                    new BigDecimal("399.00"),
                    OrderStatus.CREATED
            ));
        }

        PageResponse<OrderResponse> firstPage =
                orderService.listOrders(null, 0, 10);

        assertEquals(10, firstPage.content().size());
        assertEquals(15L, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());

        PageResponse<OrderResponse> secondPage =
                orderService.listOrders(null, 1, 10);

        assertEquals(5, secondPage.content().size());
    }

    @Test
    void shouldReturnEmptyPageWhenNoOrders() {
        PageResponse<OrderResponse> page =
                orderService.listOrders(null, 0, 10);

        assertEquals(0, page.content().size());
        assertEquals(0L, page.totalElements());
        assertEquals(0, page.totalPages());
    }

    @Test
    void shouldFilterOrdersByStatusWithPagination() {
        // 3 笔 CREATED
        for (int i = 0; i < 3; i++) {
            orderService.placeOrder(1L, 1);
        }

        // 2 笔 PAID（下单后支付）
        for (int i = 0; i < 2; i++) {
            OrderResponse placed = orderService.placeOrder(1L, 1);
            orderService.pay(placed.id());
        }

        PageResponse<OrderResponse> paidPage =
                orderService.listOrders(OrderStatus.PAID, 0, 10);

        assertEquals(2, paidPage.totalElements());
        assertEquals(2, paidPage.content().size());
        assertEquals(
                OrderStatus.PAID,
                paidPage.content().get(0).status()
        );

        PageResponse<OrderResponse> createdPage =
                orderService.listOrders(OrderStatus.CREATED, 0, 10);

        assertEquals(3, createdPage.totalElements());
    }

}
