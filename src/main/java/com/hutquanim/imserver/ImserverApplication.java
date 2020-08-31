package com.hutquanim.imserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ImserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImserverApplication.class, args);
    }

}
