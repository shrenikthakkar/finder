package com.finder.letscheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application entry point.
 *
 * EnableAsync:
 * - allows background analytics logging using @Async
 * - keeps user-facing search response fast
 */
@SpringBootApplication
@EnableAsync
public class LetscheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(LetscheckApplication.class, args);
    }
}