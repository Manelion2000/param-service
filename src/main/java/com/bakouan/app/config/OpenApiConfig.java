package com.bakouan.app.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reconciliationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reconciliation Service API")
                        .description("API de reconciliation des transactions Banque vers Moov Money")
                        .version("v1")
                        .contact(new Contact()
                                .name("Reconciliation Team")
                                .email("support@reconciliation.local"))
                        .license(new License()
                                .name("Proprietary")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Local")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project README")
                        .url("/"));
    }
}
