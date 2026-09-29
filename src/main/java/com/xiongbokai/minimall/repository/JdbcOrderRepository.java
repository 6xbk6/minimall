package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcOrderRepository implements OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Order> orderRowMapper = (
            resultSet,
            rowNumber
    ) -> new Order(
            resultSet.getLong("id"),
            resultSet.getLong("product_id"),
            resultSet.getInt("quantity"),
            resultSet.getBigDecimal("total_amount"),
            OrderStatus.valueOf(resultSet.getString("status"))
    );

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Order save(Order order) {
        String sql = """
                INSERT INTO purchase_order
                    (product_id, quantity, total_amount, status)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int affectedRows = jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    sql,
                                    new String[]{"id"}
                            );

                    statement.setLong(1, order.productId());
                    statement.setInt(2, order.quantity());
                    statement.setBigDecimal(3, order.totalAmount());
                    statement.setString(4, order.status().name());

                    return statement;
                },
                keyHolder
        );

        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "创建订单失败，实际影响行数: " + affectedRows
            );
        }

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "创建订单成功，但未获得数据库生成的订单 ID"
            );
        }

        return new Order(
                generatedId.longValue(),
                order.productId(),
                order.quantity(),
                order.totalAmount(),
                order.status()
        );
    }

    public Optional<Order> findById(Long id) {
        String sql = """
                SELECT id, product_id, quantity, total_amount, status
                FROM purchase_order
                WHERE id = ?
                """;

        List<Order> orders = jdbcTemplate.query(
                sql,
                orderRowMapper,
                id
        );

        return orders.stream().findFirst();
    }

    public List<Order> findAll() {
        String sql = """
                SELECT id, product_id, quantity, total_amount, status
                FROM purchase_order
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, orderRowMapper);
    }
}
