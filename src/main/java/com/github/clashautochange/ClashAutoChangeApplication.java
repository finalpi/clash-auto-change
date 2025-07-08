package com.github.clashautochange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClashAutoChangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClashAutoChangeApplication.class, args);
    }

}
