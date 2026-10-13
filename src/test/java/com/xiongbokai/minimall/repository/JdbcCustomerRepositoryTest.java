package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.customer.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Test
    void shouldCountCustomers() {
        long initialCount = customerRepository.count();

        customerRepository.save(new Customer(null, "alice"));

        long finalCount = customerRepository.count();

        assertEquals(initialCount + 1, finalCount);
    }

    @Test
    void shouldReturnPagedCustomersOrderedById() {
        Customer alice =
                customerRepository.save(new Customer(null, "alice"));
        Customer bob =
                customerRepository.save(new Customer(null, "bob"));

        // seed 的 demo_user + second_user + alice + bob = 4 个
        List<Customer> firstPage =
                customerRepository.findPage(0, 2);

        assertEquals(2, firstPage.size());
        assertEquals(1L, firstPage.get(0).id());
        assertEquals(2L, firstPage.get(1).id());

        // 自增 id 不回滚，所以不写死具体值，只校验按 id 升序
        List<Customer> secondPage =
                customerRepository.findPage(2, 2);

        assertEquals(2, secondPage.size());
        assertEquals(alice.id(), secondPage.get(0).id());
        assertEquals(bob.id(), secondPage.get(1).id());
    }

}
