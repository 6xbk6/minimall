package com.xiongbokai.minimall.dto.response;

import com.xiongbokai.minimall.domain.product.ProductStatus;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        ProductStatus status
) {

}
