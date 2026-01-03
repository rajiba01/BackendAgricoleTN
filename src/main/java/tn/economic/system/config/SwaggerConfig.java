package tn.economic.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

public class SwaggerConfig {

    public static OpenAPI buildOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("User API")
                        .description("API de gestion des utilisateurs")
                        .version("1.0"));
    }
}
