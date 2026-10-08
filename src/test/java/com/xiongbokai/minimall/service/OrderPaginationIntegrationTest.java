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
                orderService.listOrders(0, 10);

        assertEquals(10, firstPage.content().size());
        assertEquals(15L, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());

        PageResponse<OrderResponse> secondPage =
                orderService.listOrders(1, 10);

        assertEquals(5, secondPage.content().size());
    }

    @Test
    void shouldReturnEmptyPageWhenNoOrders() {
        PageResponse<OrderResponse> page =
                orderService.listOrders(0, 10);

        assertEquals(0, page.content().size());
        assertEquals(0L, page.totalElements());
        assertEquals(0, page.totalPages());
    }
}
