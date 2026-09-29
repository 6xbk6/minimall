package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.exception.InsufficientStockException;
import com.xiongbokai.minimall.exception.ProductNotFoundException;
import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.repository.OrderRepository;
import com.xiongbokai.minimall.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldPlaceOrderSuccessfully() {
        Product product = new Product(
                1L,
                "机械键盘",
                new BigDecimal("399.00"),
                100,
                ProductStatus.ON_SALE
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(productRepository.deductStock(1L, 2))
                .thenReturn(true);

        Order savedOrder = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CREATED
        );

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        OrderResponse response = orderService.placeOrder(1L, 2);

        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(orderCaptor.capture());

        Order orderToCreate = orderCaptor.getValue();

        assertEquals(1L, orderToCreate.productId());
        assertEquals(2, orderToCreate.quantity());
        assertEquals(
                new BigDecimal("798.00"),
                orderToCreate.totalAmount()
        );
        assertEquals(
                OrderStatus.CREATED,
                orderToCreate.status()
        );

        assertEquals(10L, response.id());
        assertEquals(
                new BigDecimal("798.00"),
                response.totalAmount()
        );
    }

    @Test
    void shouldRejectOrderWhenProductMissing() {
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> orderService.placeOrder(999L, 1)
        );

        verify(productRepository, never())
                .deductStock(any(), anyInt());
        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldRejectOrderWhenStockIsInsufficient() {
        Product product = new Product(
                2L,
                "无线鼠标",
                new BigDecimal("129.00"),
                1,
                ProductStatus.OUT_OF_STOCK
        );

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(product));
        when(productRepository.deductStock(2L, 5))
                .thenReturn(false);

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder(2L, 5)
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldRejectOrderWhenQuantityIsLessThanOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.placeOrder(1L, 0)
        );

        verify(productRepository, never())
                .findById(any());
    }
}
