package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.customer.Customer;
import com.xiongbokai.minimall.dto.response.CustomerResponse;
import com.xiongbokai.minimall.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerRegistrationIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldRegisterAndFindCustomer() {
        CustomerResponse response =
                customerService.register("alice");

        Optional<Customer> customer =
                customerRepository.findByUsername("alice");

        assertTrue(customer.isPresent());
        assertEquals(response.id(), customer.get().id());
        assertEquals("alice", customer.get().username());
    }
}
