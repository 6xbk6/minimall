package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.customer.Customer;
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
class JdbcCustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldFindCustomerById() {
        Optional<Customer> customer =
                customerRepository.findById(1L);

        assertTrue(customer.isPresent());
        assertEquals("demo_user", customer.get().username());
    }

    @Test
    void shouldReturnEmptyWhenCustomerMissing() {
        Optional<Customer> customer =
                customerRepository.findById(999L);

        assertTrue(customer.isEmpty());
    }

    @Test
    void shouldSaveCustomerAndReturnId() {
        Customer saved = customerRepository.save(
                new Customer(null, "alice")
        );

        assertTrue(saved.id() != null);
        assertEquals("alice", saved.username());
    }

    @Test
    void shouldFindCustomerByUsername() {
        customerRepository.save(new Customer(null, "alice"));

        Optional<Customer> customer =
                customerRepository.findByUsername("alice");

        assertTrue(customer.isPresent());
        assertEquals("alice", customer.get().username());
    }

    @Test
    void shouldReturnEmptyWhenUsernameMissing() {
        Optional<Customer> customer =
                customerRepository.findByUsername("nobody");

        assertTrue(customer.isEmpty());
    }

}
