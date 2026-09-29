package com.xiongbokai.minimall.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(

        @NotNull(message = "商品id不能为空")
        Long productId,

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须大于0")
        Integer quantity
) {
}
