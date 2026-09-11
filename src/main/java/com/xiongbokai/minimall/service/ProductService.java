package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import com.xiongbokai.minimall.dto.request.ProductCreateRequest;
import com.xiongbokai.minimall.dto.request.ProductPatchRequest;
import com.xiongbokai.minimall.dto.request.ProductUpdateRequest;
import com.xiongbokai.minimall.dto.response.ProductResponse;
import com.xiongbokai.minimall.exception.ProductNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {

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

    public List<ProductResponse> list(
            String keyword,
            ProductStatus status
    ) {
        return products.stream()
                .filter(product ->
                        keyword == null
                                || keyword.isBlank()
                                || product.name().contains(keyword)
                )
                .filter(product ->
                        status == null
                                || product.status() == status
                )
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getById(Long id) {
//        return new ProductResponse(
//                id,
//                "机械键盘",
//                new BigDecimal("399.00"),
//                ProductStatus.ON_SALE
//        );
//        Product product = products.stream()
//                .filter(item -> item.id().equals(id))
//                .findFirst()
//                .orElseThrow(() -> new ProductNotFoundException(id));

        return toResponse(findById(id));
    };

    public  ProductResponse create(ProductCreateRequest request) {
        Long id = idGenerator.incrementAndGet();

        Product product = new Product(
                id,
                request.name(),
                request.price(),
                ProductStatus.ON_SALE
        );

        products.add(product);

        return toResponse(product);
    }

    public ProductResponse update(
            Long id,
            ProductUpdateRequest request
    ) {
        Product existingProduct = findById(id);

        Product updatedProduct = new Product(
                existingProduct.id(),
                request.name(),
                request.price(),
                request.status()
        );

        int index = products.indexOf(existingProduct);
        products.set(index, updatedProduct);

        return toResponse(updatedProduct);
    }

    public ProductResponse patch(
            Long id,
            ProductPatchRequest request
    ) {
        Product existingProduct = findById(id);

        String newName = request.name() != null
                ? request.name()
                : existingProduct.name();

        BigDecimal newPrice = request.price() != null
                ? request.price()
                : existingProduct.price();

        ProductStatus newStatus = request.status() != null
                ? request.status()
                : existingProduct.status();

        Product updatedProduct = new Product(
                existingProduct.id(),
                newName,
                newPrice,
                newStatus
        );

        int index = products.indexOf(existingProduct);
        products.set(index, updatedProduct);

        return toResponse(updatedProduct);
    }

    public void delete(Long id) {
        Product product = findById(id);
        products.remove(product);
    }

    private Product findById(Long id) {
        return products.stream()
                .filter(product -> product.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.price(),
                product.status()
        );
    };
}
