//package com.xiongbokai.minimall.repository;
//
//import com.xiongbokai.minimall.domain.order.Order;
//import com.xiongbokai.minimall.domain.order.OrderDetail;
//import com.xiongbokai.minimall.domain.order.OrderStatus;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface OrderRepository {
//
//    Order save(Order order);
//
//    Optional<Order> findById(Long id);
//
//    List<Order> findAll();
//
//    boolean updateStatus(Long id, OrderStatus status);
//
////    List<Order> findPage(int offset, int limit);
//
////    long count();
//
////    List<Order> findPageByStatus(
////            OrderStatus status,
////            int offset,
////            int limit
////    );
////
////    long countByStatus(OrderStatus status);
//
//    List<Order> findPage(
//            OrderStatus status,
//            Long buyerId,
//            int offset,
//            int limit
//    );
//
//    long count(OrderStatus status, Long buyerId);
//
//    Optional<OrderDetail> findDetailById(Long id);
//
//}

package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderDetail;
import com.xiongbokai.minimall.domain.order.OrderItem;
import com.xiongbokai.minimall.domain.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order, List<OrderItem> items);

    Optional<Order> findById(Long id);

    List<OrderItem> findItemsByOrderId(Long orderId);

    List<Order> findAll();

    List<Long> findCreatedOrderIdsBefore(LocalDateTime cutoff);

    boolean updateStatus(Long id, OrderStatus status);

    List<Order> findPage(
            OrderStatus status,
            Long buyerId,
            int offset,
            int limit
    );

    long count(OrderStatus status, Long buyerId);

    Optional<OrderDetail> findDetailById(Long id);

}


