package com.xiongbokai.minimall.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerCreateRequest(

        @NotBlank(message = "用户名不能为空")
        String username
) {
}
