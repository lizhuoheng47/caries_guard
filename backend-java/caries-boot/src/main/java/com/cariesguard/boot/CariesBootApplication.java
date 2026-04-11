package com.cariesguard.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.cariesguard")
@MapperScan("com.cariesguard")
@ConfigurationPropertiesScan("com.cariesguard")
public class CariesBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(CariesBootApplication.class, args);
    }
}
