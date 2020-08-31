package com.hutquanim.imserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.hutquanim.imserver.mapper")
public class ImserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImserverApplication.class, args);
    }

}
