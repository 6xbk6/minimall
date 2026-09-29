package com.xiongbokai.minimall.controller;

import com.xiongbokai.minimall.dto.request.OrderCreateRequest;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderCreateRequest request
    ) {
        OrderResponse response = orderService.placeOrder(
                request.productId(),
                request.quantity()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orderService.list();
    }
}
