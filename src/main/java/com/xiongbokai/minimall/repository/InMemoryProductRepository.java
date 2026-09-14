package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final List<Product> products = new CopyOnWriteArrayList<>(
            List.of(
                    new Product(
                            1L,
                            "机械键盘",
                            new BigDecimal("399.00"),
                            ProductStatus.ON_SALE
                    ),
                    new Product(
                            2L,
                            "无线鼠标",
                            new BigDecimal("129.00"),
                            ProductStatus.OUT_OF_STOCK
                    ),
                    new Product(
                            3L,
                            "显示器支架",
                            new BigDecimal("259.00"),
                            ProductStatus.OFF_SHELF
                    )
            )
    );

    private final AtomicLong idGenerator = new AtomicLong(3L);

    @Override
    public List<Product> findAll() {
        return List.copyOf(products);
        // 提供一个不可修改的列表副本视图，让调用方可以查询和遍历，但不能直接破坏 Repository 内部的数据结构
    }

    @Override
    public Optional<Product> findById(Long id) {
        return products.stream()
                .filter(product -> product.id().equals(id))
                .findFirst();
    }

    @Override
    public Product save(Product product) {
        if (product.id() == null) {
            Product savedProduct = new Product(
                    idGenerator.incrementAndGet(),
                    product.name(),
                    product.price(),
                    product.status()
            );

            products.add(savedProduct);
            return savedProduct;
        }

        Product existingProduct = findById(product.id())
                .orElseThrow();

        int index = products.indexOf(existingProduct);
        products.set(index, product);

        return product;
    }

    @Override
    public void delete(Product product) {
        products.remove(product);
    }
}