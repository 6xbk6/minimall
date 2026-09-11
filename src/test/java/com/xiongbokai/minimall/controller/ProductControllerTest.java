package com.xiongbokai.minimall.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD
)
class ProductControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    ProductControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldGetProductById() throws Exception {
        mockMvc.perform(
                        get("/api/products/{id}", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("机械键盘"))
                .andExpect(jsonPath("$.price").value(399.00))
                .andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    @Test
    void shouldCreateProduct() throws Exception {
        String requestBody = """
            {
              "name": "人体工学键盘",
              "price": 699.00
            }
            """;

        mockMvc.perform(
                        post("/api/products")
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
                .andExpect(jsonPath("$.name").value("人体工学键盘"))
                .andExpect(jsonPath("$.price").value(699.00))
                .andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    @Test
    void shouldUpdateEntireProduct() throws Exception {
        String requestBody = """
            {
              "name": "机械键盘 Pro",
              "price": 599.00,
              "status": "OFF_SHELF"
            }
            """;

        mockMvc.perform(
                        put("/api/products/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("机械键盘 Pro"))
                .andExpect(jsonPath("$.price").value(599.00))
                .andExpect(jsonPath("$.status").value("OFF_SHELF"));

        mockMvc.perform(
                        get("/api/products/{id}", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("机械键盘 Pro"))
                .andExpect(jsonPath("$.price").value(599.00))
                .andExpect(jsonPath("$.status").value("OFF_SHELF"));
    }

    @Test
    void shouldPatchOnlyProvidedProductFields() throws Exception {
        String requestBody = """
            {
              "price": 199.00
            }
            """;

        mockMvc.perform(
                        patch("/api/products/{id}", 2L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("无线鼠标"))
                .andExpect(jsonPath("$.price").value(199.00))
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"));

        mockMvc.perform(
                        get("/api/products/{id}", 2L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("无线鼠标"))
                .andExpect(jsonPath("$.price").value(199.00))
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"));
    }

    @Test
    void shouldRejectEmptyPatchRequest() throws Exception {
        String requestBody = """
            {}
            """;

        mockMvc.perform(
                        patch("/api/products/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(
                        jsonPath("$.message").value(
                                "参数校验失败，字段: anyFieldProvided，原因: 至少提供一个需要修改的字段"
                        )
                )
                .andExpect(jsonPath("$.path").value("/api/products/1"));

        mockMvc.perform(
                        get("/api/products/{id}", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("机械键盘"))
                .andExpect(jsonPath("$.price").value(399.00))
                .andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        mockMvc.perform(
                        delete("/api/products/{id}", 3L)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(
                        get("/api/products/{id}", 3L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/products/3"));
    }

    @Test
    void shouldReturnValidationErrorWhenCreatingProductWithInvalidPrice()
            throws Exception {

        String requestBody = """
            {
              "name": "测试商品",
              "price": 0
            }
            """;

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(
                        jsonPath("$.message").value(
                                "参数校验失败，字段: price，原因: 商品价格必须大于等于0.01"
                        )
                )
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void shouldReturnMalformedJsonWhenRequestBodyIsInvalid()
            throws Exception {

        String requestBody = """
            {
              "name": "测试商品"
              "price": 99.00
            }
            """;

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                .andExpect(
                        jsonPath("$.message").value(
                                "请求体格式错误或字段值不合法"
                        )
                )
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void shouldReturnInvalidParameterWhenProductIdIsNotNumber()
            throws Exception {

        mockMvc.perform(
                        get("/api/products/{id}", "abc")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(
                        jsonPath("$.message").value(
                                "请求参数类型错误，参数: id，值: abc"
                        )
                )
                .andExpect(jsonPath("$.path").value("/api/products/abc"));
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(
                        get("/api/products/{id}", 999L)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("商品不存在，id： 999"))
                .andExpect(jsonPath("$.path").value("/api/products/999"));
    }
}