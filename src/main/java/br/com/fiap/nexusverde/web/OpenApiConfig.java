package br.com.fiap.nexusverde.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {
    @Bean
    OpenAPI nexusOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Nexus Verde").version("1.0")
                        .description("API de distribuição de água e solicitações de atendimento."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    OperationCustomizer autenticacaoOpenApi() {
        return (operacao, metodo) -> {
            if (!metodo.getBeanType().getSimpleName().equals("AuthController")) {
                operacao.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                        .addList("bearerAuth"));
            }
            return operacao;
        };
    }
}
