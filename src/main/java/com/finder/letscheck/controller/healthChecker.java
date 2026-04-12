package com.finder.letscheck.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class healthChecker {
    
    @GetMapping("/health")
    public String checkHealth() {
        return "Spotzy System is up and running!";
    }
    
}
