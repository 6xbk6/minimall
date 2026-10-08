package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.AfterTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcProductRepositoryTest {

    private final JdbcProductRepository repository;

    @Autowired
    JdbcProductRepositoryTest(
            JdbcProductRepository repository
    ) {
        this.repository = repository;
    }

    @Test
    void shouldFindAllProducts() {
        List<Product> products = repository.findAll();

        assertEquals(3, products.size());

        Product firstProduct = products.getFirst();

        assertEquals(1L, firstProduct.id());
        assertEquals("机械键盘", firstProduct.name());
        assertEquals(
                new BigDecimal("399.00"),
                firstProduct.price()
        );
        assertEquals(
                ProductStatus.ON_SALE,
                firstProduct.status()
        );
        assertEquals(
                100,
                firstProduct.stock()
        );
    }

    @Test
    void shouldFindProductById() {
        Optional<Product> productOptional =
                repository.findById(2L);

        assertTrue(productOptional.isPresent());

        Product product = productOptional.get();

        assertEquals(2L, product.id());
        assertEquals("无线鼠标", product.name());
        assertEquals(
                new BigDecimal("129.00"),
                product.price()
        );
        assertEquals(
                ProductStatus.OUT_OF_STOCK,
                product.status()
        );
        assertEquals(
                0,
                product.stock()
        );
    }

    @Test
    void shouldReturnEmptyWhenProductDoesNotExist() {
        Optional<Product> productOptional =
                repository.findById(999L);

        assertTrue(productOptional.isEmpty());
    }

    @Test
    void shouldInsertProductAndGenerateId() {
        Product newProduct = new Product(
                null,
                "人体工学键盘",
                new BigDecimal("699.00"),
                20,
                ProductStatus.ON_SALE
        );

        Product savedProduct = repository.save(newProduct);

        assertEquals(
                20,
                savedProduct.stock()
        );
        assertNotNull(savedProduct.id());
        assertTrue(savedProduct.id() > 3L);
        assertEquals("人体工学键盘", savedProduct.name());
        assertEquals(
                new BigDecimal("699.00"),
                savedProduct.price()
        );
        assertEquals(
                ProductStatus.ON_SALE,
                savedProduct.status()
        );

        Optional<Product> productOptional =
                repository.findById(savedProduct.id());

        assertTrue(productOptional.isPresent());

        Product productFromDatabase = productOptional.get();

        assertEquals(savedProduct, productFromDatabase);
    }

    @Test
    void shouldUpdateProduct() {
        Product updatedProduct = new Product(
                1L,
                "机械键盘 Pro",
                new BigDecimal("599.00"),
                50,
                ProductStatus.OFF_SHELF
        );

        Product savedProduct = repository.save(updatedProduct);

        assertEquals(updatedProduct, savedProduct);

        Product productFromDatabase = repository
                .findById(1L)
                .orElseThrow();

        assertEquals(
                50,
                productFromDatabase.stock()
        );
        assertEquals(1L, productFromDatabase.id());
        assertEquals(
                "机械键盘 Pro",
                productFromDatabase.name()
        );
        assertEquals(
                new BigDecimal("599.00"),
                productFromDatabase.price()
        );
        assertEquals(
                ProductStatus.OFF_SHELF,
                productFromDatabase.status()
        );
    }

    @Test
    void shouldDeleteProduct() {
        Product product = repository
                .findById(3L)
                .orElseThrow();

        repository.delete(product);

        Optional<Product> deletedProduct =
                repository.findById(3L);

        assertTrue(deletedProduct.isEmpty());
        assertEquals(2, repository.findAll().size());
    }

    @AfterTransaction
    void shouldRollbackDatabaseChanges() {
        List<Product> products = repository.findAll();

        assertEquals(3, products.size());

        Product firstProduct = repository
                .findById(1L)
                .orElseThrow();

        assertEquals("机械键盘", firstProduct.name());
        assertEquals(
                new BigDecimal("399.00"),
                firstProduct.price()
        );
        assertEquals(
                ProductStatus.ON_SALE,
                firstProduct.status()
        );

        assertTrue(repository.findById(3L).isPresent());
    }

    @Test
    void shouldCountProducts() {
        long initialCount = repository.count();

        Product newProduct = new Product(
                null,
                "测试商品",
                new BigDecimal("9.90"),
                5,
                ProductStatus.ON_SALE
        );

        repository.save(newProduct);

        long finalCount = repository.count();

        assertEquals(initialCount + 1, finalCount);
    }

    @Test
    void shouldReturnPagedProductsOrderedById() {
        List<Product> firstPage = repository.findPage(0, 2);

        assertEquals(2, firstPage.size());
        assertEquals(1L, firstPage.get(0).id());
        assertEquals(2L, firstPage.get(1).id());

        List<Product> secondPage = repository.findPage(2, 2);

        assertEquals(1, secondPage.size());
        assertEquals(3L, secondPage.get(0).id());
    }

}