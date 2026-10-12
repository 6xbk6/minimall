package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.customer.Customer;

import java.util.Optional;

public interface CustomerRepository {

    Optional<Customer> findById(Long id);
}
