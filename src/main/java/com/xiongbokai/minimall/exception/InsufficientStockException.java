package com.xiongbokai.minimall.exception;

public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final int currentStock;
    private final int requestedQuantity;

    public InsufficientStockException(
            Long productId,
            int currentStock,
            int requestedQuantity
    ) {
        super("库存不足，商品id： " + productId
                + "，当前库存： " + currentStock
                + "，请求扣减： " + requestedQuantity);
        this.productId = productId;
        this.currentStock = currentStock;
        this.requestedQuantity = requestedQuantity;
    }

    public Long getProductId() {
        return productId;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}
