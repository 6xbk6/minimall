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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CustomerControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    CustomerControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldRegisterCustomer() throws Exception {
        String requestBody = """
            {
              "username": "alice"
            }
            """;

        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void shouldReturn409WhenUsernameTaken() throws Exception {
        String requestBody = """
            {
              "username": "alice"
            }
            """;

        // 第一次注册成功
        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated());

        // 同名再注册 → 409 冲突
        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("CUSTOMER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.path")
                        .value("/api/customers"));
    }

    @Test
    void shouldReturn400WhenUsernameBlank() throws Exception {
        String requestBody = """
            {
              "username": "   "
            }
            """;

        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    @Test
    void shouldListCustomers() throws Exception {
        // seed 有 demo_user、second_user，再注册 alice → 共 3 个
        String registerBody = """
        {
          "username": "alice"
        }
        """;

        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void shouldReturnCustomerDetail() throws Exception {
        mockMvc.perform(get("/api/customers/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("demo_user"));
    }

    @Test
    void shouldReturn404WhenCustomerMissing() throws Exception {
        mockMvc.perform(get("/api/customers/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.path")
                        .value("/api/customers/999"));
    }

}
