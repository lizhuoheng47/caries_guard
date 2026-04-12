package com.cariesguard.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.cariesguard")
@MapperScan(basePackages = {
        "com.cariesguard.system.infrastructure.mapper",
        "com.cariesguard.patient.infrastructure.mapper",
        "com.cariesguard.image.infrastructure.mapper",
        "com.cariesguard.analysis.infrastructure.mapper",
        "com.cariesguard.report.infrastructure.mapper"
})
@ConfigurationPropertiesScan("com.cariesguard")
public class CariesBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(CariesBootApplication.class, args);
    }
}
