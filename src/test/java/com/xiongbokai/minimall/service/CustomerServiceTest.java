package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.customer.Customer;
import com.xiongbokai.minimall.dto.response.CustomerResponse;
import com.xiongbokai.minimall.exception.CustomerAlreadyExistsException;
import com.xiongbokai.minimall.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void shouldRegisterCustomer() {
        when(customerRepository.save(any(Customer.class)))
                .thenReturn(new Customer(2L, "alice"));

        CustomerResponse response =
                customerService.register("alice");

        assertEquals(2L, response.id());
        assertEquals("alice", response.username());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(customerRepository.findByUsername("alice"))
                .thenReturn(Optional.of(
                        new Customer(1L, "alice")
                ));

        assertThrows(
                CustomerAlreadyExistsException.class,
                () -> customerService.register("alice")
        );

        // 查重失败，绝不写入
        verify(customerRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectBlankUsername() {
        assertThrows(
                IllegalArgumentException.class,
                () -> customerService.register("   ")
        );

        verify(customerRepository, never())
                .findByUsername(any());
        verify(customerRepository, never())
                .save(any());
    }
}
