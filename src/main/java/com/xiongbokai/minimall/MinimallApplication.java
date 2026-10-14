package com.xiongbokai.minimall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MinimallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimallApplication.class, args);
    }

}
