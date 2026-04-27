package ara.project.takalo.shared.infrastructure.config;

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
    public OpenAPI takaloOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Takalo API")
                        .description("API REST de l'application Takalo — gestion des catégories, produits et achats.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Équipe Takalo")
                                .email("contact@takalo.local"))
                        .license(new License().name("Propriétaire")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Environnement local")
                ));
    }
}