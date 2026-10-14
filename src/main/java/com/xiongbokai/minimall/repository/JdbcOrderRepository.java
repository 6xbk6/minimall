package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.order.Order;
import com.xiongbokai.minimall.domain.order.OrderDetail;
import com.xiongbokai.minimall.domain.order.OrderItem;
import com.xiongbokai.minimall.domain.order.OrderItemDetail;
import com.xiongbokai.minimall.domain.order.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
            resultSet.getLong("buyer_id"),
            resultSet.getBigDecimal("total_amount"),
            OrderStatus.valueOf(resultSet.getString("status"))
    );

    private final RowMapper<OrderItem> orderItemRowMapper = (
            resultSet,
            rowNumber
    ) -> new OrderItem(
            resultSet.getLong("id"),
            resultSet.getLong("order_id"),
            resultSet.getLong("product_id"),
            resultSet.getInt("quantity"),
            resultSet.getBigDecimal("price"),
            resultSet.getBigDecimal("subtotal")
    );

    private final RowMapper<OrderItemDetail> orderItemDetailRowMapper = (
            resultSet,
            rowNumber
    ) -> new OrderItemDetail(
            resultSet.getLong("id"),
            resultSet.getLong("product_id"),
            resultSet.getString("product_name"),
            resultSet.getInt("quantity"),
            resultSet.getBigDecimal("price"),
            resultSet.getBigDecimal("subtotal")
    );

    private final RowMapper<OrderDetail> orderDetailHeaderRowMapper = (
            resultSet,
            rowNumber
    ) -> new OrderDetail(
            resultSet.getLong("id"),
            resultSet.getLong("buyer_id"),
            resultSet.getString("buyer_name"),
            resultSet.getBigDecimal("total_amount"),
            OrderStatus.valueOf(resultSet.getString("status")),
            null
    );

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Order save(Order order, List<OrderItem> items) {
        // 第 1 步：插订单头，拿回自增 id
        String orderSql = """
                INSERT INTO purchase_order (buyer_id, total_amount, status)
                VALUES (?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int affectedRows = jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    orderSql,
                                    new String[]{"id"}
                            );

                    statement.setLong(1, order.buyerId());
                    statement.setBigDecimal(2, order.totalAmount());
                    statement.setString(3, order.status().name());

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

        Long orderId = generatedId.longValue();

        // 第 2 步：循环插明细（每条明细都挂这个订单 id）
        String itemSql = """
                INSERT INTO order_item
                    (order_id, product_id, quantity, price, subtotal)
                VALUES (?, ?, ?, ?, ?)
                """;

        for (OrderItem item : items) {
            int itemAffectedRows = jdbcTemplate.update(
                    itemSql,
                    orderId,
                    item.productId(),
                    item.quantity(),
                    item.price(),
                    item.subtotal()
            );

            if (itemAffectedRows != 1) {
                throw new IllegalStateException(
                        "创建订单明细失败，商品id： " + item.productId()
                );
            }
        }

        return new Order(
                orderId,
                order.buyerId(),
                order.totalAmount(),
                order.status()
        );
    }

    @Override
    public Optional<Order> findById(Long id) {
        String sql = """
                SELECT id, buyer_id, total_amount, status
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

    @Override
    public List<OrderItem> findItemsByOrderId(Long orderId) {
        String sql = """
                SELECT id, order_id, product_id, quantity, price, subtotal
                FROM order_item
                WHERE order_id = ?
                ORDER BY id
                """;

        return jdbcTemplate.query(
                sql,
                orderItemRowMapper,
                orderId
        );
    }

    @Override
    public List<Order> findAll() {
        String sql = """
                SELECT id, buyer_id, total_amount, status
                FROM purchase_order
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, orderRowMapper);
    }

    @Override
    public boolean updateStatus(Long id, OrderStatus status) {
        String sql = """
                UPDATE purchase_order
                SET status = ?
                WHERE id = ?
                """;

        int affectedRows = jdbcTemplate.update(
                sql,
                status.name(),
                id
        );

        return affectedRows == 1;
    }

    @Override
    public List<Order> findPage(
            OrderStatus status,
            Long buyerId,
            int offset,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, buyer_id, total_amount, status
            FROM purchase_order
            WHERE 1 = 1
            """);

        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }

        if (buyerId != null) {
            sql.append(" AND buyer_id = ?");
            params.add(buyerId);
        }

        sql.append(" ORDER BY id LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(
                sql.toString(),
                orderRowMapper,
                params.toArray()
        );
    }

    @Override
    public long count(OrderStatus status, Long buyerId) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM purchase_order
            WHERE 1 = 1
            """);

        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }

        if (buyerId != null) {
            sql.append(" AND buyer_id = ?");
            params.add(buyerId);
        }

        Long count = jdbcTemplate.queryForObject(
                sql.toString(),
                Long.class,
                params.toArray()
        );

        return count == null ? 0L : count;
    }

    @Override
    public Optional<OrderDetail> findDetailById(Long id) {
        // 第 1 步：查订单头（JOIN 买家拿名字）
        String orderSql = """
                SELECT o.id,
                       o.buyer_id,
                       c.username AS buyer_name,
                       o.total_amount,
                       o.status
                FROM purchase_order o
                JOIN customer c ON c.id = o.buyer_id
                WHERE o.id = ?
                """;

        List<OrderDetail> headers = jdbcTemplate.query(
                orderSql,
                orderDetailHeaderRowMapper,
                id
        );

        if (headers.isEmpty()) {
            return Optional.empty();
        }

        // 第 2 步：查明细（JOIN 商品拿名字）
        String itemSql = """
                SELECT i.id,
                       i.product_id,
                       p.name AS product_name,
                       i.quantity,
                       i.price,
                       i.subtotal
                FROM order_item i
                JOIN product p ON p.id = i.product_id
                WHERE i.order_id = ?
                ORDER BY i.id
                """;

        List<OrderItemDetail> items = jdbcTemplate.query(
                itemSql,
                orderItemDetailRowMapper,
                id
        );

        OrderDetail header = headers.get(0);

        return Optional.of(new OrderDetail(
                header.id(),
                header.buyerId(),
                header.buyerName(),
                header.totalAmount(),
                header.status(),
                items
        ));
    }

    @Override
    public List<Long> findCreatedOrderIdsBefore(LocalDateTime cutoff) {
        String sql = """
                SELECT id
                FROM purchase_order
                WHERE status = 'CREATED'
                  AND created_at < ?
                ORDER BY id
                """;
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> resultSet.getLong("id"),
                cutoff
        );
    }

}
