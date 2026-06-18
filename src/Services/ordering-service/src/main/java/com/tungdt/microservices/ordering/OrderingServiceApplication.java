package com.tungdt.microservices.ordering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tungdt.microservices")
public class OrderingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderingServiceApplication.class, args);
    }
}
