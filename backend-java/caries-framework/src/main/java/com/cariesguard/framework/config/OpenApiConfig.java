package com.cariesguard.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cariesGuardOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CariesGuard Backend API")
                .description("Backend baseline APIs for the CariesGuard platform")
                .version("v1")
                .contact(new Contact().name("CariesGuard Team")));
    }
}
