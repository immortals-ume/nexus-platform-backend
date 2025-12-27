package com.immortals.platform.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for Inventory Service.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Inventory Service API")
                .description("REST API for inventory management, stock tracking, and reservations")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Platform Team")
                    .email("platform@immortals.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
