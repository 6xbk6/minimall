package com.xiongbokai.minimall.controller;

import com.xiongbokai.minimall.domain.order.OrderStatus;
import com.xiongbokai.minimall.dto.request.OrderCreateRequest;
import com.xiongbokai.minimall.dto.response.OrderDetailResponse;
import com.xiongbokai.minimall.dto.response.OrderResponse;
import com.xiongbokai.minimall.dto.response.PageResponse;
import com.xiongbokai.minimall.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

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

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(
            @PathVariable(name = "id") Long id
    ) {
        return orderService.cancel(id);
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(
            @PathVariable(name = "id") Long id
    ) {
        return orderService.pay(id);
    }

    @PostMapping("/{id}/refund")
    public OrderResponse refund(
            @PathVariable(name = "id") Long id
    ) {
        return orderService.refund(id);
    }


//    @GetMapping
//    public List<OrderResponse> list() {
//        return orderService.list();
//    }

    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(name = "status", required = false)
            OrderStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return orderService.listOrders(status, page, size);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse getOrderDetail(
            @PathVariable(name = "id") Long id
    ) {
        return orderService.getOrderDetail(id);
    }

}
