package com.xiongbokai.minimall.exception;

import com.xiongbokai.minimall.domain.order.OrderStatus;

public class IllegalOrderStatusException extends RuntimeException {

    public IllegalOrderStatusException(
            Long orderId,
            OrderStatus currentStatus,
            String expectedAction
    ) {
        super("订单状态不允许执行「" + expectedAction
                + "」，订单id： " + orderId
                + "，当前状态： " + currentStatus);
    }
}
