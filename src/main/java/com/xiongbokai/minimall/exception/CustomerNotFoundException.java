package com.xiongbokai.minimall.exception;

public class CustomerNotFoundException extends RuntimeException {

    private final Long customerId;

    public CustomerNotFoundException(Long customerId) {
        super("买家不存在，id： " + customerId);
        this.customerId = customerId;
    }

    public Long getCustomerId() {
        return customerId;
    }
}
