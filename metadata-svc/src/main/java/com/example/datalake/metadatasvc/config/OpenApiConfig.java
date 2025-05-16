package com.example.datalake.metadatasvc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI metadataOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Datalake Metadata Service API")
                        .description("APIs for metadata registration, version management, schema querying, and more.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Yuqi Guo")
                                .email("yuqi.guo@gmail.com"))
                        .license(new License().name("Apache 2.0")));
    }

}
