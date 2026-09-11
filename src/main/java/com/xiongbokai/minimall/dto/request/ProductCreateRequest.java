package com.xiongbokai.minimall.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "商品名称不能为空")
        @Size(max = 100, message = "商品名称不能超过100个字符")
        String name,

        @NotNull(message = "商品价格不能为空")
        @DecimalMin(
                value = "0.01",
                message = "商品价格必须大于等于0.01"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "商品价格最多10位整数和2位小数"
        )
        BigDecimal price
) {
}