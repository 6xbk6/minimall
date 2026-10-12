package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.customer.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
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

    @Override
    public Customer save(Customer customer) {
        String sql = """
            INSERT INTO customer (username)
            VALUES (?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int affectedRows = jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    sql,
                                    new String[]{"id"}
                            );

                    statement.setString(1, customer.username());

                    return statement;
                },
                keyHolder
        );

        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "创建买家失败，实际影响行数: " + affectedRows
            );
        }

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "创建买家成功，但未获得数据库生成的 ID"
            );
        }

        return new Customer(
                generatedId.longValue(),
                customer.username()
        );
    }

    @Override
    public Optional<Customer> findByUsername(String username) {
        String sql = """
            SELECT id, username
            FROM customer
            WHERE username = ?
            """;

        List<Customer> customers = jdbcTemplate.query(
                sql,
                customerRowMapper,
                username
        );

        return customers.stream().findFirst();
    }


}
