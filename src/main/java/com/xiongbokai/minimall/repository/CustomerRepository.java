package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.customer.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

    Optional<Customer> findById(Long id);

    Customer save(Customer customer);

    Optional<Customer> findByUsername(String username);

    List<Customer> findPage(int offset, int limit);

    long count();

}
