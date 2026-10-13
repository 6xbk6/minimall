package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderDetail;
import com.xiongbokai.minimall.domain.order.OrderItem;
import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.dto.request.OrderItemRequest;
import com.xiongbokai.minimall.dto.response.OrderDetailResponse;
import com.xiongbokai.minimall.dto.response.OrderItemResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            CustomerRepository customerRepository
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public OrderResponse placeOrder(
            Long buyerId,
            List<OrderItemRequest> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "订单明细不能为空"
            );
        }

        // 1. 校验买家存在
        customerRepository.findById(buyerId)
                .orElseThrow(() ->
                        new CustomerNotFoundException(buyerId)
                );

        // 2. 逐件商品：查商品 → 原子扣库存 → 记下明细
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest item : items) {
            Long productId = item.productId();
            int quantity = item.quantity();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() ->
                            new ProductNotFoundException(productId)
                    );

            boolean deducted = productRepository.deductStock(
                    productId,
                    quantity
            );

            if (!deducted) {
                throw new InsufficientStockException(
                        productId,
                        product.stock(),
                        quantity
                );
            }

            BigDecimal subtotal = product.price()
                    .multiply(BigDecimal.valueOf(quantity));

            orderItems.add(new OrderItem(
                    null,
                    null,
                    productId,
                    quantity,
                    product.price(),
                    subtotal
            ));

            totalAmount = totalAmount.add(subtotal);
        }

        // 3. 插订单头 + 明细（同一事务）
        Order order = new Order(
                null,
                buyerId,
                totalAmount,
                OrderStatus.CREATED
        );

        Order savedOrder = orderRepository.save(order, orderItems);

        return new OrderResponse(
                savedOrder.id(),
                savedOrder.buyerId(),
                savedOrder.totalAmount(),
                savedOrder.status()
        );
    }

    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(orderId)
                );

        if (order.status() != OrderStatus.CREATED) {
            throw new IllegalOrderStatusException(
                    orderId,
                    order.status(),
                    "取消订单"
            );
        }

        // 逐件还库存（订单可能有多件商品）
        List<OrderItem> items =
                orderRepository.findItemsByOrderId(orderId);

        for (OrderItem item : items) {
            boolean restocked = productRepository.restock(
                    item.productId(),
                    item.quantity()
            );

            if (!restocked) {
                throw new IllegalStateException(
                        "还库存失败，商品不存在，商品id： "
                                + item.productId()
                );
            }
        }

        boolean statusUpdated = orderRepository.updateStatus(
                orderId,
                OrderStatus.CANCELLED
        );

        if (!statusUpdated) {
            throw new IllegalStateException(
                    "更新订单状态失败，订单id： " + orderId
            );
        }

        return new OrderResponse(
                order.id(),
                order.buyerId(),
                order.totalAmount(),
                OrderStatus.CANCELLED
        );
    }

    @Transactional
    public OrderResponse pay(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(orderId)
                );

        if (order.status() != OrderStatus.CREATED) {
            throw new IllegalOrderStatusException(
                    orderId,
                    order.status(),
                    "支付订单"
            );
        }

        boolean statusUpdated = orderRepository.updateStatus(
                orderId,
                OrderStatus.PAID
        );

        if (!statusUpdated) {
            throw new IllegalStateException(
                    "更新订单状态失败，订单id： " + orderId
            );
        }

        return new OrderResponse(
                order.id(),
                order.buyerId(),
                order.totalAmount(),
                OrderStatus.PAID
        );
    }

    @Transactional
    public OrderResponse refund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(orderId)
                );

        if (order.status() != OrderStatus.PAID) {
            throw new IllegalOrderStatusException(
                    orderId,
                    order.status(),
                    "退款订单"
            );
        }

        // 逐件还库存
        List<OrderItem> items =
                orderRepository.findItemsByOrderId(orderId);

        for (OrderItem item : items) {
            boolean restocked = productRepository.restock(
                    item.productId(),
                    item.quantity()
            );

            if (!restocked) {
                throw new IllegalStateException(
                        "还库存失败，商品不存在，商品id： "
                                + item.productId()
                );
            }
        }

        boolean statusUpdated = orderRepository.updateStatus(
                orderId,
                OrderStatus.REFUNDED
        );

        if (!statusUpdated) {
            throw new IllegalStateException(
                    "更新订单状态失败，订单id： " + orderId
            );
        }

        return new OrderResponse(
                order.id(),
                order.buyerId(),
                order.totalAmount(),
                OrderStatus.REFUNDED
        );
    }

    public PageResponse<OrderResponse> listOrders(
            OrderStatus status,
            Long buyerId,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "页码不能为负数，当前传入： " + page
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "每页条数必须在 1 到 100 之间，当前传入： " + size
            );
        }

        long totalElements = orderRepository.count(status, buyerId);

        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);

        List<Order> pageData = orderRepository.findPage(
                status,
                buyerId,
                page * size,
                size
        );

        List<OrderResponse> content = pageData
                .stream()
                .map(order -> new OrderResponse(
                        order.id(),
                        order.buyerId(),
                        order.totalAmount(),
                        order.status()
                ))
                .toList();

        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    public OrderDetailResponse getOrderDetail(Long orderId) {
        OrderDetail detail = orderRepository
                .findDetailById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(orderId)
                );

        List<OrderItemResponse> items = detail.items()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.productId(),
                        item.productName(),
                        item.quantity(),
                        item.price(),
                        item.subtotal()
                ))
                .toList();

        return new OrderDetailResponse(
                detail.id(),
                detail.buyerId(),
                detail.buyerName(),
                detail.totalAmount(),
                detail.status(),
                items
        );
    }

}
