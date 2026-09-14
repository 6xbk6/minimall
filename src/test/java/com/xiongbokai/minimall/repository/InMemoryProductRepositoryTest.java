package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryProductRepositoryTest {

    private InMemoryProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
    }

    @Test
    void shouldFindInitialProducts() {
        List<Product> products = repository.findAll();

        assertEquals(3, products.size());
        assertTrue(repository.findById(1L).isPresent());
        assertTrue(repository.findById(2L).isPresent());
        assertTrue(repository.findById(3L).isPresent());
    }

    @Test
    void shouldSaveNewProduct() {
        Product product = new Product(
                null,
                "人体工学键盘",
                new BigDecimal("699.00"),
                ProductStatus.ON_SALE
        );

        Product savedProduct = repository.save(product);

        assertEquals(4L, savedProduct.id());
        assertEquals("人体工学键盘", savedProduct.name());
        assertEquals(
                new BigDecimal("699.00"),
                savedProduct.price()
        );
        assertEquals(
                ProductStatus.ON_SALE,
                savedProduct.status()
        );
        assertEquals(
                savedProduct,
                repository.findById(4L).orElseThrow()
        );
    }

    @Test
    void shouldUpdateExistingProduct() {
        Product updatedProduct = new Product(
                1L,
                "机械键盘 Pro",
                new BigDecimal("599.00"),
                ProductStatus.OFF_SHELF
        );

        Product savedProduct = repository.save(updatedProduct);

        assertEquals(updatedProduct, savedProduct);
        assertEquals(
                updatedProduct,
                repository.findById(1L).orElseThrow()
        );
        assertEquals(3, repository.findAll().size());
    }

    @Test
    void shouldDeleteProduct() {
        Product product = repository.findById(3L)
                .orElseThrow();

        repository.delete(product);

        assertTrue(repository.findById(3L).isEmpty());
        assertEquals(2, repository.findAll().size());
    }
}