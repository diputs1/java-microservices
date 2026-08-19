package com.tungdt.microservices.background;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.tungdt.microservices")
public class BackgroundJobServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackgroundJobServiceApplication.class, args);
    }
}
