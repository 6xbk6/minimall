package com.xiongbokai.minimall.exception;

public class ProductNotFoundException extends RuntimeException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("商品不存在，id： " + productId);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
