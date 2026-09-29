package com.xiongbokai.minimall.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class OrderControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    OrderControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldPlaceOrder() throws Exception {
        String requestBody = """
            {
              "productId": 1,
              "quantity": 2
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(798.00))
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnErrorWhenStockIsInsufficient() throws Exception {
        String requestBody = """
            {
              "productId": 2,
              "quantity": 1
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.path").value("/api/orders"));

        // 失败后不允许残留任何订单
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenProductMissing() throws Exception {
        String requestBody = """
            {
              "productId": 999,
              "quantity": 1
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldReturnValidationErrorWhenQuantityInvalid()
            throws Exception {

        String requestBody = """
            {
              "productId": 1,
              "quantity": 0
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(
                        jsonPath("$.message").value(
                                "参数校验失败，字段: quantity，原因: 购买数量必须大于0"
                        )
                );
    }
}
