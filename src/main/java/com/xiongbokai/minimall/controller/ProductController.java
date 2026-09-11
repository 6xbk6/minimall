package com.xiongbokai.minimall.controller;

import com.xiongbokai.minimall.domain.product.Product;
import com.xiongbokai.minimall.domain.product.ProductStatus;
import com.xiongbokai.minimall.dto.request.ProductCreateRequest;
import com.xiongbokai.minimall.dto.request.ProductPatchRequest;
import com.xiongbokai.minimall.dto.request.ProductUpdateRequest;
import com.xiongbokai.minimall.dto.response.ProductResponse;
import com.xiongbokai.minimall.service.ProductService;
import jakarta.websocket.server.PathParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

//    @GetMapping("/{id}")
//    public ProductResponse getById(
//            @PathVariable (name = "id") Long id
//    ){
//        return new ProductResponse(
//                id,
//                "机械键盘",
//                new BigDecimal("399.00"),
//                "ON_SALE"
//        );
//    }

    public ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
        // 把 Spring 传进来的 ProductService，保存到当前 Controller 的字段里
    }

    @GetMapping()
    public List<ProductResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status
    ) {
        return productService.list(keyword, status);
    }

    @PostMapping()
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductResponse response = productService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable(name = "id") Long id
    ) {
        productService.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/{id}")
    public ProductResponse patch(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody ProductPatchRequest request
    ) {
        return  productService.patch(id, request);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(
            @PathVariable (name = "id") Long id
    ){
       return productService.getById(id);
    }

}
