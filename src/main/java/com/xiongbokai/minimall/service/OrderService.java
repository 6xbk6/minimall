package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderStatus;
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

}
