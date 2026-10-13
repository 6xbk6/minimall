//package com.xiongbokai.minimall.dto.request;
//
//import jakarta.validation.constraints.Min;
//import jakarta.validation.constraints.NotNull;
//
//public record OrderCreateRequest(
//
//        @NotNull(message = "买家id不能为空")
//        Long buyerId,
//
//        @NotNull(message = "商品id不能为空")
//        Long productId,
//
//        @NotNull(message = "购买数量不能为空")
//        @Min(value = 1, message = "购买数量必须大于0")
//        Integer quantity
//) {
//}

package com.xiongbokai.minimall.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCreateRequest(

        @NotNull(message = "买家id不能为空")
        Long buyerId,

        @NotEmpty(message = "订单明细不能为空")
        List<@Valid OrderItemRequest> items
) {
}
