package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.customer.Customer;
import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderDetail;
import com.xiongbokai.minimall.domain.order.OrderItem;
import com.xiongbokai.minimall.domain.order.OrderItemDetail;
import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import com.xiongbokai.minimall.dto.request.OrderItemRequest;
import com.xiongbokai.minimall.dto.response.OrderDetailResponse;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.dto.response.PageResponse;
import com.xiongbokai.minimall.exception.CustomerNotFoundException;
import com.xiongbokai.minimall.exception.IllegalOrderStatusException;
import com.xiongbokai.minimall.exception.InsufficientStockException;
import com.xiongbokai.minimall.exception.OrderNotFoundException;
import com.xiongbokai.minimall.exception.ProductNotFoundException;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
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

    private OrderItemRequest item(Long productId, int quantity) {
        return new OrderItemRequest(productId, quantity);
    }

    private Product product(
            Long id,
            String name,
            BigDecimal price,
            int stock
    ) {
        return new Product(
                id, name, price, stock, ProductStatus.ON_SALE
        );
    }

    private OrderItem orderItem(
            Long orderId,
            Long productId,
            int quantity,
            BigDecimal price
    ) {
        return new OrderItem(
                null,
                orderId,
                productId,
                quantity,
                price,
                price.multiply(BigDecimal.valueOf(quantity))
        );
    }

    @Test
    void shouldPlaceOrderWithSingleItem() {
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(
                        new Customer(1L, "demo_user")
                ));

        Product product = product(
                1L, "机械键盘", new BigDecimal("399.00"), 100
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(productRepository.deductStock(1L, 2))
                .thenReturn(true);

        Order savedOrder = new Order(
                10L, 1L, new BigDecimal("798.00"), OrderStatus.CREATED
        );

        when(orderRepository.save(any(Order.class), anyList()))
                .thenReturn(savedOrder);

        OrderResponse response =
                orderService.placeOrder(1L, List.of(item(1L, 2)));

        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<List<OrderItem>> itemsCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(orderRepository).save(
                orderCaptor.capture(),
                itemsCaptor.capture()
        );

        Order orderToCreate = orderCaptor.getValue();

        assertEquals(1L, orderToCreate.buyerId());
        assertEquals(
                new BigDecimal("798.00"),
                orderToCreate.totalAmount()
        );
        assertEquals(
                OrderStatus.CREATED,
                orderToCreate.status()
        );

        // 明细：1 件商品，小计 = 399×2
        assertEquals(1, itemsCaptor.getValue().size());
        assertEquals(1L, itemsCaptor.getValue().get(0).productId());
        assertEquals(2, itemsCaptor.getValue().get(0).quantity());
        assertEquals(
                new BigDecimal("399.00"),
                itemsCaptor.getValue().get(0).price()
        );
        assertEquals(
                new BigDecimal("798.00"),
                itemsCaptor.getValue().get(0).subtotal()
        );

        assertEquals(10L, response.id());
        assertEquals(
                new BigDecimal("798.00"),
                response.totalAmount()
        );
    }

    @Test
    void shouldPlaceOrderWithMultipleItems() {
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(
                        new Customer(1L, "demo_user")
                ));

        Product keyboard = product(
                1L, "机械键盘", new BigDecimal("399.00"), 100
        );
        Product stand = product(
                3L, "显示器支架", new BigDecimal("259.00"), 100
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(keyboard));
        when(productRepository.findById(3L))
                .thenReturn(Optional.of(stand));
        when(productRepository.deductStock(1L, 2))
                .thenReturn(true);
        when(productRepository.deductStock(3L, 1))
                .thenReturn(true);

        Order savedOrder = new Order(
                10L, 1L, new BigDecimal("1057.00"), OrderStatus.CREATED
        );

        when(orderRepository.save(any(Order.class), anyList()))
                .thenReturn(savedOrder);

        OrderResponse response = orderService.placeOrder(
                1L,
                List.of(item(1L, 2), item(3L, 1))
        );

        // 两件商品都被扣库存
        verify(productRepository).deductStock(1L, 2);
        verify(productRepository).deductStock(3L, 1);

        // 总额 = 399×2 + 259×1 = 1057
        assertEquals(
                new BigDecimal("1057.00"),
                response.totalAmount()
        );

        ArgumentCaptor<List<OrderItem>> itemsCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(orderRepository).save(
                any(Order.class),
                itemsCaptor.capture()
        );

        assertEquals(2, itemsCaptor.getValue().size());
        assertEquals(
                new BigDecimal("798.00"),
                itemsCaptor.getValue().get(0).subtotal()
        );
        assertEquals(
                new BigDecimal("259.00"),
                itemsCaptor.getValue().get(1).subtotal()
        );
    }

    @Test
    void shouldRejectOrderWhenItemsEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.placeOrder(1L, List.of())
        );

        verify(productRepository, never())
                .findById(any());
        verify(orderRepository, never())
                .save(any(), anyList());
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
                () -> orderService.placeOrder(
                        1L,
                        List.of(item(999L, 1))
                )
        );

        verify(productRepository, never())
                .deductStock(any(), anyInt());
        verify(orderRepository, never())
                .save(any(), anyList());
    }

    @Test
    void shouldRejectOrderWhenStockIsInsufficient() {
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(
                        new Customer(1L, "demo_user")
                ));

        Product product = product(
                2L, "无线鼠标", new BigDecimal("129.00"), 1
        );

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(product));
        when(productRepository.deductStock(2L, 5))
                .thenReturn(false);

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder(
                        1L,
                        List.of(item(2L, 5))
                )
        );

        verify(orderRepository, never())
                .save(any(), anyList());
    }

    @Test
    void shouldCancelCreatedOrderAndRestock() {
        Order order = new Order(
                10L, 1L, new BigDecimal("798.00"), OrderStatus.CREATED
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));
        when(orderRepository.findItemsByOrderId(10L))
                .thenReturn(List.of(
                        orderItem(10L, 1L, 2, new BigDecimal("399.00"))
                ));
        when(productRepository.restock(1L, 2))
                .thenReturn(true);
        when(orderRepository.updateStatus(
                10L, OrderStatus.CANCELLED))
                .thenReturn(true);

        OrderResponse response = orderService.cancel(10L);

        // 按明细还库存
        verify(productRepository).restock(1L, 2);
        verify(orderRepository).updateStatus(
                10L, OrderStatus.CANCELLED
        );
        assertEquals(
                OrderStatus.CANCELLED.name(),
                response.status().name()
        );
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
                10L, 1L, new BigDecimal("798.00"), OrderStatus.CANCELLED
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
                10L, 1L, new BigDecimal("798.00"), OrderStatus.PAID
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
                10L, 1L, new BigDecimal("798.00"), OrderStatus.CREATED
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));
        when(orderRepository.updateStatus(10L, OrderStatus.PAID))
                .thenReturn(true);

        OrderResponse response = orderService.pay(10L);

        verify(orderRepository).updateStatus(10L, OrderStatus.PAID);
        assertEquals(
                OrderStatus.PAID.name(),
                response.status().name()
        );
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
                10L, 1L, new BigDecimal("798.00"), OrderStatus.PAID
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
                10L, 1L, new BigDecimal("798.00"), OrderStatus.CANCELLED
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
        when(orderRepository.count(null, null)).thenReturn(15L);
        when(orderRepository.findPage(null, null, 0, 10)).thenReturn(
                List.of(
                        new Order(
                                1L, 1L,
                                new BigDecimal("399.00"),
                                OrderStatus.CREATED
                        ),
                        new Order(
                                2L, 1L,
                                new BigDecimal("399.00"),
                                OrderStatus.CREATED
                        )
                )
        );

        PageResponse<OrderResponse> page =
                orderService.listOrders(null, null, 0, 10);

        assertEquals(2, page.content().size());
        assertEquals(0, page.page());
        assertEquals(10, page.size());
        assertEquals(15L, page.totalElements());
        assertEquals(2, page.totalPages());

        verify(orderRepository).findPage(null, null, 0, 10);
    }

    @Test
    void shouldReturnSecondPageWithOffset() {
        when(orderRepository.count(null, null)).thenReturn(15L);
        when(orderRepository.findPage(null, null, 10, 10)).thenReturn(
                List.of(
                        new Order(
                                11L, 1L,
                                new BigDecimal("399.00"),
                                OrderStatus.CREATED
                        )
                )
        );

        PageResponse<OrderResponse> page =
                orderService.listOrders(null, null, 1, 10);

        assertEquals(1, page.content().size());
        assertEquals(1, page.page());
        assertEquals(2, page.totalPages());

        verify(orderRepository).findPage(null, null, 10, 10);
    }

    @Test
    void shouldRejectInvalidPageParams() {
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.listOrders(null, null, -1, 10)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.listOrders(null, null, 0, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.listOrders(null, null, 0, 101)
        );

        verify(orderRepository, never())
                .findPage(any(), any(), anyInt(), anyInt());
    }

    @Test
    void shouldFilterOrdersByStatus() {
        when(orderRepository.count(OrderStatus.PAID, null))
                .thenReturn(5L);
        when(orderRepository.findPage(
                OrderStatus.PAID, null, 0, 10
        )).thenReturn(
                List.of(new Order(
                        1L, 1L,
                        new BigDecimal("399.00"),
                        OrderStatus.PAID
                ))
        );

        PageResponse<OrderResponse> page =
                orderService.listOrders(OrderStatus.PAID, null, 0, 10);

        assertEquals(5L, page.totalElements());
        assertEquals(1, page.content().size());

        verify(orderRepository)
                .findPage(OrderStatus.PAID, null, 0, 10);
        verify(orderRepository, never())
                .findPage(null, null, 0, 10);
    }

    @Test
    void shouldListAllWhenStatusIsNull() {
        when(orderRepository.count(null, null)).thenReturn(15L);
        when(orderRepository.findPage(null, null, 0, 10)).thenReturn(
                List.of(new Order(
                        1L, 1L,
                        new BigDecimal("399.00"),
                        OrderStatus.CREATED
                ))
        );

        PageResponse<OrderResponse> page =
                orderService.listOrders(null, null, 0, 10);

        verify(orderRepository).findPage(null, null, 0, 10);
    }

    @Test
    void shouldReturnOrderDetailWithItems() {
        OrderDetail detail = new OrderDetail(
                10L,
                1L,
                "demo_user",
                new BigDecimal("798.00"),
                OrderStatus.CREATED,
                List.of(new OrderItemDetail(
                        1L,
                        1L,
                        "机械键盘",
                        2,
                        new BigDecimal("399.00"),
                        new BigDecimal("798.00")
                ))
        );

        when(orderRepository.findDetailById(10L))
                .thenReturn(Optional.of(detail));

        OrderDetailResponse response =
                orderService.getOrderDetail(10L);

        assertEquals("机械键盘",
                response.items().get(0).productName());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(
                new BigDecimal("798.00"),
                response.totalAmount()
        );
        assertEquals(OrderStatus.CREATED, response.status());
        assertEquals("demo_user", response.buyerName());
        assertEquals(1, response.items().size());
    }

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
                10L, 1L, new BigDecimal("798.00"), OrderStatus.PAID
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));
        when(orderRepository.findItemsByOrderId(10L))
                .thenReturn(List.of(
                        orderItem(10L, 1L, 2, new BigDecimal("399.00"))
                ));
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
        assertEquals(
                OrderStatus.REFUNDED.name(),
                response.status().name()
        );
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
                10L, 1L, new BigDecimal("798.00"), OrderStatus.CREATED
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
                10L, 1L, new BigDecimal("798.00"), OrderStatus.REFUNDED
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
                () -> orderService.placeOrder(
                        999L,
                        List.of(item(1L, 1))
                )
        );

        verify(productRepository, never())
                .findById(any());
        verify(orderRepository, never())
                .save(any(), anyList());
    }

    @Test
    void shouldFilterOrdersByBuyer() {
        when(orderRepository.count(null, 1L)).thenReturn(3L);
        when(orderRepository.findPage(null, 1L, 0, 10))
                .thenReturn(
                        List.of(new Order(
                                1L, 1L,
                                new BigDecimal("399.00"),
                                OrderStatus.CREATED
                        ))
                );

        PageResponse<OrderResponse> page =
                orderService.listOrders(null, 1L, 0, 10);

        assertEquals(3L, page.totalElements());

        verify(orderRepository).count(null, 1L);
        verify(orderRepository).findPage(null, 1L, 0, 10);
    }

    @Test
    void shouldFilterOrdersByStatusAndBuyerTogether() {
        when(orderRepository.count(OrderStatus.PAID, 1L))
                .thenReturn(2L);
        when(orderRepository.findPage(
                OrderStatus.PAID, 1L, 0, 10
        )).thenReturn(
                List.of(new Order(
                        1L, 1L,
                        new BigDecimal("399.00"),
                        OrderStatus.PAID
                ))
        );

        PageResponse<OrderResponse> page =
                orderService.listOrders(
                        OrderStatus.PAID, 1L, 0, 10
                );

        assertEquals(2L, page.totalElements());

        verify(orderRepository)
                .count(OrderStatus.PAID, 1L);
        verify(orderRepository)
                .findPage(OrderStatus.PAID, 1L, 0, 10);
    }

    @Test
    void shouldCancelExpiredOrdersInBatch() {
        when(orderRepository.findCreatedOrderIdsBefore(
                any(LocalDateTime.class)
        )).thenReturn(List.of(10L, 11L));

        Order first = new Order(
                10L, 1L, new BigDecimal("399.00"), OrderStatus.CREATED
        );
        Order second = new Order(
                11L, 1L, new BigDecimal("798.00"), OrderStatus.CREATED
        );

        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(first));
        when(orderRepository.findItemsByOrderId(10L))
                .thenReturn(List.of(
                        orderItem(10L, 1L, 1, new BigDecimal("399.00"))
                ));
        when(productRepository.restock(1L, 1))
                .thenReturn(true);
        when(orderRepository.updateStatus(
                10L, OrderStatus.CANCELLED))
                .thenReturn(true);

        when(orderRepository.findById(11L))
                .thenReturn(Optional.of(second));
        when(orderRepository.findItemsByOrderId(11L))
                .thenReturn(List.of(
                        orderItem(11L, 1L, 2, new BigDecimal("399.00"))
                ));
        when(productRepository.restock(1L, 2))
                .thenReturn(true);
        when(orderRepository.updateStatus(
                11L, OrderStatus.CANCELLED))
                .thenReturn(true);

        int closed = orderService.cancelExpiredOrders(30);

        assertEquals(2, closed);
        verify(productRepository).restock(1L, 1);
        verify(productRepository).restock(1L, 2);
        verify(orderRepository).updateStatus(
                10L, OrderStatus.CANCELLED);
        verify(orderRepository).updateStatus(
                11L, OrderStatus.CANCELLED);
    }

    @Test
    void shouldReturnZeroWhenNoExpiredOrders() {
        when(orderRepository.findCreatedOrderIdsBefore(
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        int closed = orderService.cancelExpiredOrders(30);

        assertEquals(0, closed);
        verify(orderRepository, never()).findById(any());
        verify(productRepository, never()).restock(any(), anyInt());
    }

}
