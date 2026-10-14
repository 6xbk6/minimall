package com.xiongbokai.minimall.scheduler;

import com.xiongbokai.minimall.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpiryScheduler {

    private final OrderService orderService;

    public OrderExpiryScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    // 每 60 秒扫一次，关闭超过 30 分钟未支付的订单
    @Scheduled(fixedDelay = 60_000)
    public void closeExpiredOrders() {
        orderService.cancelExpiredOrders(30);
    }
}
