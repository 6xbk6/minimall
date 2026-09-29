package com.xiongbokai.minimall.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockDeductRequest(

        @NotNull(message = "扣减数量不能为空")
        @Min(value = 1, message = "扣减数量必须大于0")
        Integer quantity
) {
}
