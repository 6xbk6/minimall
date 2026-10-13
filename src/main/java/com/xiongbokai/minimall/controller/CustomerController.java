package com.xiongbokai.minimall.controller;

import com.xiongbokai.minimall.dto.request.CustomerCreateRequest;
import com.xiongbokai.minimall.dto.response.CustomerResponse;
import com.xiongbokai.minimall.dto.response.PageResponse;
import com.xiongbokai.minimall.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public PageResponse<CustomerResponse> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return customerService.list(page, size);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(
            @PathVariable(name = "id") Long id
    ) {
        return customerService.getById(id);
    }

}
