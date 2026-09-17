package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JdbcProductRepository implements ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Product> productRowMapper = (
            resultSet,
            rowNumber
    ) -> new Product(
            resultSet.getLong("id"),
            resultSet.getString("name"),
            resultSet.getBigDecimal("price"),
            ProductStatus.valueOf(
                    resultSet.getString("status")
            )
    );

    public JdbcProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Product> findAll() {
        String sql = """
                SELECT id, name, price, status
                FROM product
                ORDER BY id
                """;

        return jdbcTemplate.query(
                sql,
                productRowMapper
        );
    }

    @Override
    public Optional<Product> findById(Long id) {
        String sql = """
                SELECT id, name, price, status
                FROM product
                WHERE id = ?
                """;

        List<Product> products = jdbcTemplate.query(
                sql,
                productRowMapper,
                id
        );

        return products.stream().findFirst();
    }

    @Override
    public Product save(Product product) {
//        if (product.id() != null) {
//            throw new UnsupportedOperationException(
//                    "JDBC 修改商品功能尚未实现"
//            );
//        }

        if (product.id() != null) {
            String sql = """
            UPDATE product
            SET name = ?,
                price = ?,
                status = ?
            WHERE id = ?
            """;

            int affectedRows = jdbcTemplate.update(
                    sql,
                    product.name(),
                    product.price(),
                    product.status().name(),
                    product.id()
            );

            if (affectedRows != 1) {
                throw new IllegalStateException(
                        "修改商品失败，实际影响行数: " + affectedRows
                );
            }

            return product;
        }

        String sql = """
            INSERT INTO product (name, price, status)
            VALUES (?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int affectedRows = jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    sql,
                                    new String[]{"id"}
                            );

                    statement.setString(
                            1,
                            product.name()
                    );

                    statement.setBigDecimal(
                            2,
                            product.price()
                    );

                    statement.setString(
                            3,
                            product.status().name()
                    );

                    return statement;
                },
                keyHolder
        );

        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "新增商品失败，实际影响行数: " + affectedRows
            );
        }

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "新增商品成功，但未获得数据库生成的商品 ID"
            );
        }

        return new Product(
                generatedId.longValue(),
                product.name(),
                product.price(),
                product.status()
        );
    }

//    @Override
//    public void delete(Product product) {
//        throw new UnsupportedOperationException(
//                "JDBC 删除商品功能尚未实现"
//        );
//    }

    @Override
    public void delete(Product product) {
        String sql = """
            DELETE FROM product
            WHERE id = ?
            """;

        int affectedRows = jdbcTemplate.update(
                sql,
                product.id()
        );

        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "删除商品失败，实际影响行数: " + affectedRows
            );
        }
    }
}
