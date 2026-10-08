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
        assertEquals(
                100,
                repository.findById(1L)
                        .orElseThrow()
                        .stock()
        );

        assertEquals(
                0,
                repository.findById(2L)
                        .orElseThrow()
                        .stock()
        );

        assertEquals(
                100,
                repository.findById(3L)
                        .orElseThrow()
                        .stock()
        );
    }

    @Test
    void shouldSaveNewProduct() {
        Product product = new Product(
                null,
                "人体工学键盘",
                new BigDecimal("699.00"),
                20,
                ProductStatus.ON_SALE
        );

        Product savedProduct = repository.save(product);

        assertEquals(20, savedProduct.stock());
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
                50,
                ProductStatus.OFF_SHELF
        );

        Product savedProduct = repository.save(updatedProduct);

        assertEquals(50, savedProduct.stock());
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

    @Test
    void shouldCountProductsInMemory() {
        long initialCount = repository.count();

        Product newProduct = new Product(
                null,
                "内存测试商品",
                new BigDecimal("9.90"),
                5,
                ProductStatus.ON_SALE
        );

        repository.save(newProduct);

        long finalCount = repository.count();

        assertEquals(initialCount + 1, finalCount);
    }

    @Test
    void shouldReturnPagedProductsInMemory() {
        List<Product> firstPage = repository.findPage(0, 2);

        assertEquals(2, firstPage.size());

        List<Product> secondPage = repository.findPage(2, 2);

        assertEquals(1, secondPage.size());
        assertEquals(3L, secondPage.get(0).id());
    }

}