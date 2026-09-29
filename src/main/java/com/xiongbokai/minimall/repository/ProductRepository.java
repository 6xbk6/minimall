package com.xiongbokai.minimall.repository;

import com.xiongbokai.minimall.domain.product.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<Product> findAll();

    Optional<Product> findById(Long id);

    Product save(Product product);

    boolean deductStock(Long id, int quantity);

    void delete(Product product);
}