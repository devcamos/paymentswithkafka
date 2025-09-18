package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Payment Service
 * Provides comprehensive API documentation and interactive testing interface
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort);
        devServer.setDescription("Development server");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.paymentservice.com");
        prodServer.setDescription("Production server");

        Contact contact = new Contact();
        contact.setEmail("support@paymentservice.com");
        contact.setName("Payment Service Team");
        contact.setUrl("https://www.paymentservice.com");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Payment Service API")
                .version("1.0.0")
                .contact(contact)
                .description("Comprehensive Payment Processing API with real-time WebSocket updates and Kafka-based async processing")
                .termsOfService("https://www.paymentservice.com/terms")
                .license(mitLicense);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer));
    }
}
