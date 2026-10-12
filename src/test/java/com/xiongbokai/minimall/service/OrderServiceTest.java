package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.customer.Customer;
import com.xiongbokai.minimall.domain.order.OrderDetail;
import com.xiongbokai.minimall.dto.response.OrderDetailResponse;
import com.xiongbokai.minimall.dto.response.PageResponse;
import com.xiongbokai.minimall.exception.*;
import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.repository.CustomerRepository;
import com.xiongbokai.minimall.repository.OrderRepository;
import com.xiongbokai.minimall.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
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

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldPlaceOrderSuccessfully() {
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(
                        new Customer(1L, "demo_user")
                ));

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
                OrderStatus.CREATED,
                1L
        );

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        OrderResponse response = orderService.placeOrder(1L, 1L, 2);

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
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(
                        new Customer(1L, "demo_user")
                ));

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> orderService.placeOrder(1L, 999L, 1)
        );

        verify(productRepository, never())
                .deductStock(any(), anyInt());
        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldRejectOrderWhenStockIsInsufficient() {
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(
                        new Customer(1L, "demo_user")
                ));

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
                () -> orderService.placeOrder(1L, 2L, 5)
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldRejectOrderWhenQuantityIsLessThanOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.placeOrder(1L, 1L, 0)
        );

        verify(productRepository, never())
                .findById(any());
    }

    @Test
    void shouldCancelCreatedOrderAndRestock() {
        Order order = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CREATED,
                1L
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));
        when(productRepository.restock(1L, 2))
                .thenReturn(true);
        when(orderRepository.updateStatus(
                10L, OrderStatus.CANCELLED))
                .thenReturn(true);

        OrderResponse response = orderService.cancel(10L);

        verify(productRepository).restock(1L, 2);
        verify(orderRepository).updateStatus(
                10L, OrderStatus.CANCELLED
        );
        assertEquals(OrderStatus.CANCELLED.name(),
                response.status().name());
    }

    @Test
    void shouldRejectCancelWhenOrderMissing() {
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.cancel(999L)
        );

        verify(productRepository, never())
                .restock(any(), anyInt());
        verify(orderRepository, never())
                .updateStatus(any(), any());
    }

    @Test
    void shouldRejectCancelWhenOrderAlreadyCancelled() {
        Order order = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CANCELLED,
                1L
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalOrderStatusException.class,
                () -> orderService.cancel(10L)
        );

        verify(productRepository, never())
                .restock(any(), anyInt());
        verify(orderRepository, never())
                .updateStatus(any(), any());
    }

    @Test
    void shouldRejectCancelWhenOrderPaid() {
        Order order = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.PAID,
                1L
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalOrderStatusException.class,
                () -> orderService.cancel(10L)
        );

        verify(productRepository, never())
                .restock(any(), anyInt());
    }

    @Test
    void shouldPayCreatedOrder() {
        Order order = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CREATED,
                1L
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));
        when(orderRepository.updateStatus(10L, OrderStatus.PAID))
                .thenReturn(true);

        OrderResponse response = orderService.pay(10L);

        verify(orderRepository).updateStatus(10L, OrderStatus.PAID);
        assertEquals(OrderStatus.PAID.name(),
                response.status().name());
    }

    @Test
    void shouldRejectPayWhenOrderMissing() {
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.pay(999L)
        );

        verify(orderRepository, never())
                .updateStatus(any(), any());
    }

    @Test
    void shouldRejectPayWhenOrderAlreadyPaid() {
        Order order = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.PAID,
                1L
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalOrderStatusException.class,
                () -> orderService.pay(10L)
        );

        verify(orderRepository, never())
                .updateStatus(any(), any());
    }

    @Test
    void shouldRejectPayWhenOrderCancelled() {
        Order order = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CANCELLED,
                1L
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalOrderStatusException.class,
                () -> orderService.pay(10L)
        );

        verify(orderRepository, never())
                .updateStatus(any(), any());
    }

    @Test
    void shouldReturnFirstPage() {
        when(orderRepository.count()).thenReturn(15L);
        when(orderRepository.findPage(0, 10)).thenReturn(
                List.of(
                        new Order(1L, 1L, 1,
                                new BigDecimal("399.00"),
                                OrderStatus.CREATED, 1L),
                        new Order(2L, 1L, 1,
                                new BigDecimal("399.00"),
                                OrderStatus.CREATED, 1L)
                )
        );

        PageResponse<OrderResponse> page =
                orderService.listOrders(null, 0, 10);

        assertEquals(2, page.content().size());
        assertEquals(0, page.page());
        assertEquals(10, page.size());
        assertEquals(15L, page.totalElements());
        assertEquals(2, page.totalPages());

        verify(orderRepository).findPage(0, 10);
    }

    @Test
    void shouldReturnSecondPageWithOffset() {
        when(orderRepository.count()).thenReturn(15L);
        when(orderRepository.findPage(10, 10)).thenReturn(
                List.of(
                        new Order(11L, 1L, 1,
                                new BigDecimal("399.00"),
                                OrderStatus.CREATED, 1L)
                )
        );

        PageResponse<OrderResponse> page =
                orderService.listOrders(null, 1, 10);

        assertEquals(1, page.content().size());
        assertEquals(1, page.page());
        assertEquals(2, page.totalPages());

        // 关键：第二页的 OFFSET 必须是 10
        verify(orderRepository).findPage(10, 10);
    }

    @Test
    void shouldRejectInvalidPageParams() {
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.listOrders(null, -1, 10)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.listOrders(null, 0, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.listOrders(null, 0, 101)
        );

        verify(orderRepository, never()).findPage(anyInt(), anyInt());
    }

    @Test
    void shouldFilterOrdersByStatus() {
        when(orderRepository.countByStatus(OrderStatus.PAID))
                .thenReturn(5L);
        when(orderRepository.findPageByStatus(
                OrderStatus.PAID, 0, 10
        )).thenReturn(
                List.of(new Order(
                        1L, 1L, 1,
                        new BigDecimal("399.00"),
                        OrderStatus.PAID,
                        1L
                ))
        );

        PageResponse<OrderResponse> page =
                orderService.listOrders(OrderStatus.PAID, 0, 10);

        assertEquals(5L, page.totalElements());
        assertEquals(1, page.content().size());

        // 关键：走了筛选方法，绝不能走全量方法
        verify(orderRepository)
                .findPageByStatus(OrderStatus.PAID, 0, 10);
        verify(orderRepository, never())
                .findPage(anyInt(), anyInt());
        verify(orderRepository, never())
                .count();
    }

    @Test
    void shouldListAllWhenStatusIsNull() {
        when(orderRepository.count()).thenReturn(15L);
        when(orderRepository.findPage(0, 10)).thenReturn(
                List.of(new Order(
                        1L, 1L, 1,
                        new BigDecimal("399.00"),
                        OrderStatus.CREATED,
                        1L
                ))
        );

        PageResponse<OrderResponse> page =
                orderService.listOrders(null, 0, 10);

        // 关键：没传状态，只能走全量方法
        verify(orderRepository).findPage(0, 10);
        verify(orderRepository, never())
                .findPageByStatus(any(), anyInt(), anyInt());
    }

//    @Test
//    void shouldReturnOrderDetailWithProductName() {
//        Order order = new Order(
//                10L,
//                1L,
//                2,
//                new BigDecimal("798.00"),
//                OrderStatus.CREATED
//        );
//
//        Product product = new Product(
//                1L,
//                "机械键盘",
//                new BigDecimal("399.00"),
//                100,
//                ProductStatus.ON_SALE
//        );
//
//        when(orderRepository.findById(10L))
//                .thenReturn(Optional.of(order));
//        when(productRepository.findById(1L))
//                .thenReturn(Optional.of(product));
//
//        OrderDetailResponse detail =
//                orderService.getOrderDetail(10L);
//
//        assertEquals("机械键盘", detail.productName());
//        assertEquals(2, detail.quantity());
//        assertEquals(new BigDecimal("798.00"),
//                detail.totalAmount());
//        assertEquals(OrderStatus.CREATED, detail.status());
//    }

    @Test
    void shouldReturnOrderDetailWithProductName() {
//        OrderDetail detail = new OrderDetail(
//                10L,
//                1L,
//                "机械键盘",
//                2,
//                new BigDecimal("798.00"),
//                OrderStatus.CREATED
//        );

        OrderDetail detail = new OrderDetail(
                10L,
                1L,
                "机械键盘",
                2,
                new BigDecimal("798.00"),
                OrderStatus.CREATED,
                1L,
                "demo_user"
        );

        when(orderRepository.findDetailById(10L))
                .thenReturn(Optional.of(detail));

        OrderDetailResponse response =
                orderService.getOrderDetail(10L);

        assertEquals("机械键盘", response.productName());
        assertEquals(2, response.quantity());
        assertEquals(new BigDecimal("798.00"),
                response.totalAmount());
        assertEquals(OrderStatus.CREATED, response.status());
        assertEquals("demo_user", response.buyerName());
    }


//    @Test
//    void shouldThrowWhenOrderMissingForDetail() {
//        when(orderRepository.findById(999L))
//                .thenReturn(Optional.empty());
//
//        assertThrows(
//                OrderNotFoundException.class,
//                () -> orderService.getOrderDetail(999L)
//        );
//
//        // 订单都没有，就不该再查商品
//        verify(productRepository, never()).findById(any());
//    }

    @Test
    void shouldThrowWhenOrderMissingForDetail() {
        when(orderRepository.findDetailById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderDetail(999L)
        );
    }

    @Test
    void shouldRefundPaidOrderAndRestock() {
        Order order = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.PAID,
                1L
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));
        when(productRepository.restock(1L, 2))
                .thenReturn(true);
        when(orderRepository.updateStatus(
                10L, OrderStatus.REFUNDED))
                .thenReturn(true);

        OrderResponse response = orderService.refund(10L);

        verify(productRepository).restock(1L, 2);
        verify(orderRepository).updateStatus(
                10L, OrderStatus.REFUNDED
        );
        assertEquals(OrderStatus.REFUNDED.name(),
                response.status().name());
    }

    @Test
    void shouldRejectRefundWhenOrderMissing() {
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.refund(999L)
        );

        verify(productRepository, never())
                .restock(any(), anyInt());
        verify(orderRepository, never())
                .updateStatus(any(), any());
    }

    @Test
    void shouldRejectRefundWhenOrderCreated() {
        Order order = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.CREATED,
                1L
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalOrderStatusException.class,
                () -> orderService.refund(10L)
        );

        verify(productRepository, never())
                .restock(any(), anyInt());
        verify(orderRepository, never())
                .updateStatus(any(), any());
    }

    @Test
    void shouldRejectRefundWhenOrderAlreadyRefunded() {
        Order order = new Order(
                10L,
                1L,
                2,
                new BigDecimal("798.00"),
                OrderStatus.REFUNDED,
                1L
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalOrderStatusException.class,
                () -> orderService.refund(10L)
        );

        verify(productRepository, never())
                .restock(any(), anyInt());
        verify(orderRepository, never())
                .updateStatus(any(), any());
    }

    @Test
    void shouldRejectPlaceOrderWhenBuyerMissing() {
        when(customerRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                CustomerNotFoundException.class,
                () -> orderService.placeOrder(999L, 1L, 1)
        );

        verify(productRepository, never())
                .findById(any());
        verify(orderRepository, never())
                .save(any());
    }

}
