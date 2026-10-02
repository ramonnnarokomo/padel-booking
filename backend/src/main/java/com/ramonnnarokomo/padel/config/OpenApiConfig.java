package com.ramonnnarokomo.padel.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata for the generated OpenAPI document. Swagger UI: http://localhost:8080/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI padelBookOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PadelBook API")
                .version("v1")
                .description("Reservas de pistas de pádel: horarios, presupuesto, reservas y cancelaciones."));
    }
}
