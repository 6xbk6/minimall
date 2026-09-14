package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import com.xiongbokai.minimall.dto.response.ProductResponse;
import com.xiongbokai.minimall.repository.ProductRepository;
import com.xiongbokai.minimall.exception.ProductNotFoundException;
import com.xiongbokai.minimall.dto.request.ProductCreateRequest;
import com.xiongbokai.minimall.dto.request.ProductPatchRequest;
import com.xiongbokai.minimall.dto.request.ProductUpdateRequest;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldGetProductById() {
        Product product = new Product(
                1L,
                "机械键盘",
                new BigDecimal("399.00"),
                ProductStatus.ON_SALE
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponse response = productService.getById(1L);

        assertEquals(1L, response.id());
        assertEquals("机械键盘", response.name());
        assertEquals(
                new BigDecimal("399.00"),
                response.price()
        );
        assertEquals(
                ProductStatus.ON_SALE,
                response.status()
        );

        verify(productRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getById(999L)
        );

        assertEquals(999L, exception.getProductId());
        assertEquals(
                "商品不存在，id： 999",
                exception.getMessage()
        );

        verify(productRepository).findById(999L);
    }

    @Test
    void shouldCreateProductWithDefaultStatus() {
        ProductCreateRequest request = new ProductCreateRequest(
                "人体工学键盘",
                new BigDecimal("699.00")
        );

        Product savedProduct = new Product(
                4L,
                "人体工学键盘",
                new BigDecimal("699.00"),
                ProductStatus.ON_SALE
        );

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response = productService.create(request);

        ArgumentCaptor<Product> productCaptor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).save(productCaptor.capture());

        Product productToSave = productCaptor.getValue();

        assertNull(productToSave.id());
        assertEquals("人体工学键盘", productToSave.name());
        assertEquals(
                new BigDecimal("699.00"),
                productToSave.price()
        );
        assertEquals(
                ProductStatus.ON_SALE,
                productToSave.status()
        );

        assertEquals(4L, response.id());
        assertEquals("人体工学键盘", response.name());
        assertEquals(
                new BigDecimal("699.00"),
                response.price()
        );
        assertEquals(
                ProductStatus.ON_SALE,
                response.status()
        );
    }

    @Test
    void shouldPatchOnlyProvidedProductFields() {
        Product existingProduct = new Product(
                2L,
                "无线鼠标",
                new BigDecimal("129.00"),
                ProductStatus.OUT_OF_STOCK
        );

        ProductPatchRequest request = new ProductPatchRequest(
                null,
                new BigDecimal("199.00"),
                null
        );

        Product savedProduct = new Product(
                2L,
                "无线鼠标",
                new BigDecimal("199.00"),
                ProductStatus.OUT_OF_STOCK
        );

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response = productService.patch(2L, request);

        ArgumentCaptor<Product> productCaptor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).findById(2L);
        verify(productRepository).save(productCaptor.capture());

        Product productToSave = productCaptor.getValue();

        assertEquals(2L, productToSave.id());
        assertEquals("无线鼠标", productToSave.name());
        assertEquals(
                new BigDecimal("199.00"),
                productToSave.price()
        );
        assertEquals(
                ProductStatus.OUT_OF_STOCK,
                productToSave.status()
        );

        assertEquals(2L, response.id());
        assertEquals("无线鼠标", response.name());
        assertEquals(
                new BigDecimal("199.00"),
                response.price()
        );
        assertEquals(
                ProductStatus.OUT_OF_STOCK,
                response.status()
        );
    }

    @Test
    void shouldDeleteExistingProduct() {
        Product product = new Product(
                3L,
                "显示器支架",
                new BigDecimal("259.00"),
                ProductStatus.OFF_SHELF
        );

        when(productRepository.findById(3L))
                .thenReturn(Optional.of(product));

        productService.delete(3L);

        verify(productRepository).findById(3L);
        verify(productRepository).delete(product);
    }

    @Test
    void shouldNotDeleteWhenProductDoesNotExist() {
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.delete(999L)
        );

        assertEquals(999L, exception.getProductId());
        assertEquals(
                "商品不存在，id： 999",
                exception.getMessage()
        );

        verify(productRepository).findById(999L);
        verify(productRepository, never())
                .delete(any(Product.class));
    }

    @Test
    void shouldFilterProductsByKeywordAndStatus() {
        List<Product> products = List.of(
                new Product(
                        1L,
                        "机械键盘",
                        new BigDecimal("399.00"),
                        ProductStatus.ON_SALE
                ),
                new Product(
                        2L,
                        "无线键盘",
                        new BigDecimal("299.00"),
                        ProductStatus.OUT_OF_STOCK
                ),
                new Product(
                        3L,
                        "机械鼠标",
                        new BigDecimal("199.00"),
                        ProductStatus.ON_SALE
                ),
                new Product(
                        4L,
                        "显示器支架",
                        new BigDecimal("259.00"),
                        ProductStatus.OFF_SHELF
                )
        );

        when(productRepository.findAll())
                .thenReturn(products);

        List<ProductResponse> responses = productService.list(
                "键盘",
                ProductStatus.ON_SALE
        );

        assertEquals(1, responses.size());

        ProductResponse response = responses.getFirst();

        assertEquals(1L, response.id());
        assertEquals("机械键盘", response.name());
        assertEquals(
                new BigDecimal("399.00"),
                response.price()
        );
        assertEquals(
                ProductStatus.ON_SALE,
                response.status()
        );

        verify(productRepository).findAll();
    }

    @Test
    void shouldUpdateEntireProduct() {
        Product existingProduct = new Product(
                1L,
                "机械键盘",
                new BigDecimal("399.00"),
                ProductStatus.ON_SALE
        );

        ProductUpdateRequest request = new ProductUpdateRequest(
                "机械键盘 Pro",
                new BigDecimal("599.00"),
                ProductStatus.OFF_SHELF
        );

        Product savedProduct = new Product(
                1L,
                "机械键盘 Pro",
                new BigDecimal("599.00"),
                ProductStatus.OFF_SHELF
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response = productService.update(1L, request);

        ArgumentCaptor<Product> productCaptor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).findById(1L);
        verify(productRepository).save(productCaptor.capture());

        Product productToSave = productCaptor.getValue();

        assertEquals(1L, productToSave.id());
        assertEquals("机械键盘 Pro", productToSave.name());
        assertEquals(
                new BigDecimal("599.00"),
                productToSave.price()
        );
        assertEquals(
                ProductStatus.OFF_SHELF,
                productToSave.status()
        );

        assertEquals(1L, response.id());
        assertEquals("机械键盘 Pro", response.name());
        assertEquals(
                new BigDecimal("599.00"),
                response.price()
        );
        assertEquals(
                ProductStatus.OFF_SHELF,
                response.status()
        );
    }
}