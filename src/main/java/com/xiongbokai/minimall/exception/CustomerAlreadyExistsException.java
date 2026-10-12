package com.xiongbokai.minimall.exception;

public class CustomerAlreadyExistsException
        extends RuntimeException {

    private final String username;

    public CustomerAlreadyExistsException(String username) {
        super("用户名已存在： " + username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
