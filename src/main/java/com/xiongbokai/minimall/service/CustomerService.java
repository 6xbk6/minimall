package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.customer.Customer;
import com.xiongbokai.minimall.dto.response.CustomerResponse;
import com.xiongbokai.minimall.exception.CustomerAlreadyExistsException;
import com.xiongbokai.minimall.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(
            CustomerRepository customerRepository
    ) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse register(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "用户名不能为空"
            );
        }

        String trimmed = username.trim();

        customerRepository.findByUsername(trimmed)
                .ifPresent(existing -> {
                    throw new CustomerAlreadyExistsException(
                            trimmed
                    );
                });

        Customer saved = customerRepository.save(
                new Customer(null, trimmed)
        );

        return new CustomerResponse(
                saved.id(),
                saved.username()
        );
    }
}
