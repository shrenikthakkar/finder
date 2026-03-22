package com.finder.letscheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LetscheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(LetscheckApplication.class, args);
        System.out.println("Letscheck backend started successfully");
    }

}
