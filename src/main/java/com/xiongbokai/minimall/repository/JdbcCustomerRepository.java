package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.customer.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCustomerRepository implements CustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Customer> customerRowMapper = (
            resultSet,
            rowNumber
    ) -> new Customer(
            resultSet.getLong("id"),
            resultSet.getString("username")
    );

    public JdbcCustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        String sql = """
                SELECT id, username
                FROM customer
                WHERE id = ?
                """;

        List<Customer> customers = jdbcTemplate.query(
                sql,
                customerRowMapper,
                id
        );

        return customers.stream().findFirst();
    }
}
