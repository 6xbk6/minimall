package com.xiongbokai.minimall.dto.request;

import com.xiongbokai.minimall.domain.product.ProductStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductPatchRequest(

        @Pattern(
                regexp = ".*\\S.*",
                message = "商品名称不能为空或只包含空格"
        )
        @Size(
                max = 100,
                message = "商品名称不能超过100个字符"
        )
        String name,

        @DecimalMin(
                value = "0.01",
                message = "商品价格必须大于等于0.01"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "商品价格最多10位整数和2位小数"
        )
        BigDecimal price,

        ProductStatus status
) {
    @AssertTrue(message = "至少提供一个需要修改的字段")
    public boolean isAnyFieldProvided() {
        return name != null
                || price != null
                || status != null;
    }
}