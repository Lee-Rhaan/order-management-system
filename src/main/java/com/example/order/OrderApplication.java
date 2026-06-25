package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Order Management System application.
 */
@SpringBootApplication
public class OrderApplication {

    /**
     * Bootstraps the Spring Boot application.
     *
     * @param args runtime arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}