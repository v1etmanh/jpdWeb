package com.jpd.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JpDwebSwpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JpDwebSwpApplication.class, args);
    }

}
