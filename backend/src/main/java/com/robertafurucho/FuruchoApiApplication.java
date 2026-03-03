package com.robertafurucho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Furucho API backend.
 * 
 * This Spring Boot application provides a REST API for managing
 * handmade doll orders for Roberta Furucho's artisan business.
 */
@SpringBootApplication
@EnableScheduling
public class FuruchoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuruchoApiApplication.class, args);
    }
}
