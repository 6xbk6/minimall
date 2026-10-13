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

    // 下单：买家 1 买 2 件机械键盘（399×2 = 798）
    private String placeKeyboard2() {
        return """
            {
              "buyerId": 1,
              "items": [
                {"productId": 1, "quantity": 2}
              ]
            }
            """;
    }

    // 下单：买家 1 买 1 件机械键盘
    private String placeKeyboard1() {
        return """
            {
              "buyerId": 1,
              "items": [
                {"productId": 1, "quantity": 1}
              ]
            }
            """;
    }

    @Test
    void shouldPlaceOrder() throws Exception {
        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard2())
                )
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.buyerId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(798.00))
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldPlaceOrderWithMultipleItems() throws Exception {
        // 一单两件：机械键盘 399 + 显示器支架 259 = 658
        String requestBody = """
            {
              "buyerId": 1,
              "items": [
                {"productId": 1, "quantity": 1},
                {"productId": 3, "quantity": 1}
              ]
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(658.00))
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldRejectEmptyItems() throws Exception {
        String requestBody = """
            {
              "buyerId": 1,
              "items": []
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
                .andExpect(jsonPath("$.path").value("/api/orders"));

        // 校验失败不允许残留任何订单
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldReturnErrorWhenStockIsInsufficient() throws Exception {
        String requestBody = """
            {
              "buyerId": 1,
              "items": [
                {"productId": 2, "quantity": 1}
              ]
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
              "buyerId": 1,
              "items": [
                {"productId": 999, "quantity": 1}
              ]
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
              "buyerId": 1,
              "items": [
                {"productId": 1, "quantity": 0}
              ]
            }
            """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    @Test
    void shouldCancelOrderAndRestoreStock() throws Exception {
        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard2())
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
        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard2())
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
        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard2())
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
        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard2())
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
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(placeKeyboard1())
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
            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(placeKeyboard1())
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

    @Test
    void shouldFilterOrdersByStatus() throws Exception {
        // 第一单：CREATED
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeKeyboard1()))
                .andExpect(status().isCreated());

        // 第二单：下单后支付，变 PAID
        MvcResult secondPlace = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard1())
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number secondIdNumber = JsonPath.read(
                secondPlace.getResponse().getContentAsString(),
                "$.id"
        );
        Long secondId = secondIdNumber.longValue();

        mockMvc.perform(post("/api/orders/{id}/pay", secondId))
                .andExpect(status().isOk());

        // 筛选 PAID：只有 1 笔
        mockMvc.perform(get("/api/orders")
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // 筛选 CREATED：只有 1 笔
        mockMvc.perform(get("/api/orders")
                        .param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CREATED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnAllOrdersWhenStatusAbsent() throws Exception {
        // 建 1 笔 CREATED
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeKeyboard1()))
                .andExpect(status().isCreated());

        // 建 1 笔 PAID
        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard1())
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                placeResult.getResponse().getContentAsString(),
                "$.id"
        );
        Long orderId = orderIdNumber.longValue();

        mockMvc.perform(post("/api/orders/{id}/pay", orderId))
                .andExpect(status().isOk());

        // 不传 status → 混合全查，2 笔都在
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldReturn400WhenStatusInvalid() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("status", "ABC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    void shouldReturnOrderDetail() throws Exception {
        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard2())
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                placeResult.getResponse().getContentAsString(),
                "$.id"
        );
        Long orderId = orderIdNumber.longValue();

        // 查详情：订单信息 + 明细里的商品名都在
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.buyerId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(798.00))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("机械键盘"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].price").value(399.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(798.00));
    }

    @Test
    void shouldReturn404WhenOrderDetailMissing() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/orders/999"));
    }

    @Test
    void shouldRefundPaidOrder() throws Exception {
        // 下单 + 支付
        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard2())
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                placeResult.getResponse().getContentAsString(),
                "$.id"
        );
        Long orderId = orderIdNumber.longValue();

        mockMvc.perform(post("/api/orders/{id}/pay", orderId))
                .andExpect(status().isOk());

        // 退款
        mockMvc.perform(
                        post("/api/orders/{id}/refund", orderId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        // 库存还回 100
        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(100));
    }

    @Test
    void shouldRejectRefundingCreatedOrder() throws Exception {
        // 只下单不支付：状态 CREATED
        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard2())
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                placeResult.getResponse().getContentAsString(),
                "$.id"
        );
        Long orderId = orderIdNumber.longValue();

        // 退款被拒
        mockMvc.perform(
                        post("/api/orders/{id}/refund", orderId)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("ILLEGAL_ORDER_STATUS"));

        // 库存保持扣减后的 98，没有被错误还回
        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(98));
    }

    @Test
    void shouldReturn404WhenBuyerMissingOnPlaceOrder() throws Exception {
        String requestBody = """
        {
          "buyerId": 999,
          "items": [
            {"productId": 1, "quantity": 1}
          ]
        }
        """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    void shouldReturnOrderDetailWithBuyerName() throws Exception {
        MvcResult placeResult = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard2())
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                placeResult.getResponse().getContentAsString(),
                "$.id"
        );
        Long orderId = orderIdNumber.longValue();

        // 详情带买家名
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyerId").value(1))
                .andExpect(jsonPath("$.buyerName").value("demo_user"));
    }

    @Test
    void shouldFilterOrdersByBuyer() throws Exception {
        // 买家 1 下一单
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeKeyboard1()))
                .andExpect(status().isCreated());

        // 买家 2 下一单
        String buyer2Body = """
        {
          "buyerId": 2,
          "items": [
            {"productId": 1, "quantity": 1}
          ]
        }
        """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buyer2Body))
                .andExpect(status().isCreated());

        // 按买家 1 筛选：只有 1 笔
        mockMvc.perform(get("/api/orders")
                        .param("buyerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].buyerId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterOrdersByStatusAndBuyerTogether() throws Exception {
        // 买家 1：先下一单（CREATED）
        MvcResult firstPlace = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard1())
                )
                .andExpect(status().isCreated())
                .andReturn();

        // 买家 1：再下一单并支付（PAID）
        MvcResult secondPlace = mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(placeKeyboard1())
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number paidIdNumber = JsonPath.read(
                secondPlace.getResponse().getContentAsString(),
                "$.id"
        );
        Long paidOrderId = paidIdNumber.longValue();

        mockMvc.perform(post("/api/orders/{id}/pay", paidOrderId))
                .andExpect(status().isOk());

        // 组合：买家 1 + PAID → 只有 1 笔，且是支付过的那笔
        mockMvc.perform(get("/api/orders")
                        .param("buyerId", "1")
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(paidOrderId))
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

}
