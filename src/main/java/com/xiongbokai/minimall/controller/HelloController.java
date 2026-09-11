package com.xiongbokai.minimall.controller;

import com.xiongbokai.minimall.dto.response.HelloResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
//    public String hello() {
//        return "Hello, MiniMall!";
//    }
    public HelloResponse hello(
        @RequestParam(
                name = "name",
                defaultValue = "Frontend Developer"
        )
        String name
    ){
        return new HelloResponse(
                "Hello," + name + "!",
                "MiniMall"
        );
    }
}
