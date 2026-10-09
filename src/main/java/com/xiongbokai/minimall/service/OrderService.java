package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.dto.response.PageResponse;
import com.xiongbokai.minimall.exception.IllegalOrderStatusException;
import com.xiongbokai.minimall.exception.OrderNotFoundException;
import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.exception.InsufficientStockException;
import com.xiongbokai.minimall.exception.ProductNotFoundException;
import com.xiongbokai.minimall.repository.OrderRepository;
import com.xiongbokai.minimall.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderService(
            ProductRepository productRepository,
            OrderRepository orderRepository
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse placeOrder(
            Long productId,
            int quantity
    ) {
        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "购买数量必须大于0"
            );
        }

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

        BigDecimal totalAmount = product.price()
                .multiply(BigDecimal.valueOf(quantity));

        Order order = new Order(
                null,
                productId,
                quantity,
                totalAmount,
                OrderStatus.CREATED
        );

        Order savedOrder = orderRepository.save(order);

        return new OrderResponse(
                savedOrder.id(),
                savedOrder.productId(),
                savedOrder.quantity(),
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

        boolean restocked = productRepository.restock(
                order.productId(),
                order.quantity()
        );

        if (!restocked) {
            throw new IllegalStateException(
                    "还库存失败，商品不存在，商品id： "
                            + order.productId()
            );
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

        Order cancelledOrder = new Order(
                order.id(),
                order.productId(),
                order.quantity(),
                order.totalAmount(),
                OrderStatus.CANCELLED
        );

        return new OrderResponse(
                cancelledOrder.id(),
                cancelledOrder.productId(),
                cancelledOrder.quantity(),
                cancelledOrder.totalAmount(),
                cancelledOrder.status()
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

        Order paidOrder = new Order(
                order.id(),
                order.productId(),
                order.quantity(),
                order.totalAmount(),
                OrderStatus.PAID
        );

        return new OrderResponse(
                paidOrder.id(),
                paidOrder.productId(),
                paidOrder.quantity(),
                paidOrder.totalAmount(),
                paidOrder.status()
        );
    }

    public List<OrderResponse> list() {
        return orderRepository.findAll()
                .stream()
                .map(order -> new OrderResponse(
                        order.id(),
                        order.productId(),
                        order.quantity(),
                        order.totalAmount(),
                        order.status()
                ))
                .toList();
    }

//    public PageResponse<OrderResponse> listOrders(
//            int page,
//            int size
//    ) {
//        if (page < 0) {
//            throw new IllegalArgumentException(
//                    "页码不能为负数，当前传入： " + page
//            );
//        }
//
//        if (size < 1 || size > 100) {
//            throw new IllegalArgumentException(
//                    "每页条数必须在 1 到 100 之间，当前传入： " + size
//            );
//        }
//
//        long totalElements = orderRepository.count();
//
//        int totalPages = totalElements == 0
//                ? 0
//                : (int) Math.ceil((double) totalElements / size);
//
//        int offset = page * size;
//
//        List<OrderResponse> content = orderRepository
//                .findPage(offset, size)
//                .stream()
//                .map(order -> new OrderResponse(
//                        order.id(),
//                        order.productId(),
//                        order.quantity(),
//                        order.totalAmount(),
//                        order.status()
//                ))
//                .toList();
//
//        return new PageResponse<>(
//                content,
//                page,
//                size,
//                totalElements,
//                totalPages
//        );
//    }

    public PageResponse<OrderResponse> listOrders(
            OrderStatus status,
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

        long totalElements;
        List<Order> pageData;

        if (status == null) {
            // 没传状态 → 查全部（走原方法）
            totalElements = orderRepository.count();
            pageData = orderRepository.findPage(
                    page * size,
                    size
            );
        } else {
            // 传了状态 → 带筛选
            totalElements = orderRepository.countByStatus(status);
            pageData = orderRepository.findPageByStatus(
                    status,
                    page * size,
                    size
            );
        }

        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);

        List<OrderResponse> content = pageData
                .stream()
                .map(order -> new OrderResponse(
                        order.id(),
                        order.productId(),
                        order.quantity(),
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

}
