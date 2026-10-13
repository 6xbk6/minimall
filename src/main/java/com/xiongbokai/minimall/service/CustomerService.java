package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.customer.Customer;
import com.xiongbokai.minimall.dto.response.CustomerResponse;
import com.xiongbokai.minimall.dto.response.PageResponse;
import com.xiongbokai.minimall.exception.CustomerAlreadyExistsException;
import com.xiongbokai.minimall.exception.CustomerNotFoundException;
import com.xiongbokai.minimall.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public PageResponse<CustomerResponse> list(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "页码不能为负数，当前传入： " + page
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "每页条数必须在 1 到 100 之间，当前传入： " + size
            );
        }

        long totalElements = customerRepository.count();

        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);

        int offset = page * size;

        List<CustomerResponse> content = customerRepository
                .findPage(offset, size)
                .stream()
                .map(customer -> new CustomerResponse(
                        customer.id(),
                        customer.username()
                ))
                .toList();

        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    public CustomerResponse getById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        new CustomerNotFoundException(id)
                );

        return new CustomerResponse(
                customer.id(),
                customer.username()
        );
    }

}
