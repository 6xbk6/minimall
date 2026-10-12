package com.xiongbokai.minimall.controller;

import com.xiongbokai.minimall.dto.request.CustomerCreateRequest;
import com.xiongbokai.minimall.dto.response.CustomerResponse;
import com.xiongbokai.minimall.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> register(
            @Valid @RequestBody CustomerCreateRequest request
    ) {
        CustomerResponse response = customerService.register(
                request.username()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
