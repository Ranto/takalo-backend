package ara.project.takalo.shared.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";

    @Bean
    public OpenAPI takaloOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Takalo API")
                        .description("API REST de l'application Takalo — gestion des catégories, produits, achats et utilisateurs (RBAC).")
                        .version("v1")
                        .contact(new Contact()
                                .name("Équipe Takalo")
                                .email("contact@takalo.local"))
                        .license(new License().name("Propriétaire")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Environnement local")
                ))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT émis par Keycloak (realm \"takalo\")")));
    }
}
