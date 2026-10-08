package com.xiongbokai.minimall.controller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MvcResult;
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
                .andExpect(jsonPath("$.content.length()").value(1));
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
                .andExpect(jsonPath("$.content.length()").value(0));
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

    @Test
    void shouldCancelOrderAndRestoreStock() throws Exception {
        // 先下单
        String placeBody = """
        {
          "productId": 1,
          "quantity": 2
        }
        """;

        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                placeResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long orderId = orderIdNumber.longValue();


        // 取消
        mockMvc.perform(
                        post("/api/orders/{id}/cancel", orderId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 订单还在列表里，但状态是 CANCELLED
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));

        // 库存已还回 100
        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(100));
    }

    @Test
    void shouldReturn404WhenCancellingMissingOrder() throws Exception {
        mockMvc.perform(
                        post("/api/orders/{id}/cancel", 999L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.path")
                        .value("/api/orders/999/cancel"));
    }

    @Test
    void shouldRejectCancellingOrderTwice() throws Exception {
        String placeBody = """
        {
          "productId": 1,
          "quantity": 2
        }
        """;

        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                placeResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long orderId = orderIdNumber.longValue();


        // 第一次取消成功
        mockMvc.perform(
                        post("/api/orders/{id}/cancel", orderId)
                )
                .andExpect(status().isOk());

        // 第二次取消被拒
        mockMvc.perform(
                        post("/api/orders/{id}/cancel", orderId)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("ILLEGAL_ORDER_STATUS"));

        // 库存只能是 100，绝不能因为取消两次被还成 102
        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(100));
    }

    @Test
    void shouldPayOrder() throws Exception {
        String placeBody = """
        {
          "productId": 1,
          "quantity": 2
        }
        """;

        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                placeResult.getResponse().getContentAsString(),
                "$.id"
        );
        Long orderId = orderIdNumber.longValue();

        // 支付
        mockMvc.perform(
                        post("/api/orders/{id}/pay", orderId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PAID"));

        // 支付不动库存，仍是扣减后的 98
        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(98));
    }

    @Test
    void shouldReturn404WhenPayingMissingOrder() throws Exception {
        mockMvc.perform(
                        post("/api/orders/{id}/pay", 999L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.path")
                        .value("/api/orders/999/pay"));
    }

    @Test
    void shouldRejectPayingOrderTwice() throws Exception {
        String placeBody = """
        {
          "productId": 1,
          "quantity": 2
        }
        """;

        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                placeResult.getResponse().getContentAsString(),
                "$.id"
        );
        Long orderId = orderIdNumber.longValue();

        // 第一次支付成功
        mockMvc.perform(
                        post("/api/orders/{id}/pay", orderId)
                )
                .andExpect(status().isOk());

        // 第二次支付被拒
        mockMvc.perform(
                        post("/api/orders/{id}/pay", orderId)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("ILLEGAL_ORDER_STATUS"));

        // 库存仍是 98，支付不碰库存
        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(98));
    }

    @Test
    void shouldReturnDefaultFirstPage() throws Exception {
        // 先建 3 笔订单
        for (int i = 0; i < 3; i++) {
            String placeBody = """
            {
              "productId": 1,
              "quantity": 1
            }
            """;

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(placeBody)
                    )
                    .andExpect(status().isCreated());
        }

        // 不传参数 → 默认第一页，每页 10
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldReturnRequestedPage() throws Exception {
        for (int i = 0; i < 3; i++) {
            String placeBody = """
            {
              "productId": 1,
              "quantity": 1
            }
            """;

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(placeBody)
                    )
                    .andExpect(status().isCreated());
        }

        // 第 0 页，每页 2 条 → 2 条
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        // 第 1 页，每页 2 条 → 剩 1 条
        mockMvc.perform(get("/api/orders")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldReturn400WhenSizeInvalid() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETER_INVALID"))
                .andExpect(jsonPath("$.path").value("/api/orders"));
    }


}
