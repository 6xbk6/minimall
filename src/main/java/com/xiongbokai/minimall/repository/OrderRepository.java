package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    List<Order> findAll();

    boolean updateStatus(Long id, OrderStatus status);

    List<Order> findPage(int offset, int limit);

    long count();

}
