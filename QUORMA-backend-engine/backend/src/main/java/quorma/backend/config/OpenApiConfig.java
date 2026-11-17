package quorma.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata. Every endpoint requires a Bearer JWT unless it opts out with {@code @SecurityRequirements}. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("QUORMA Backend API")
                        .version("1.0.0")
                        .description("QUORMA game management API."))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /** A secured operation that documents no client error gets the two the security layer can return. */
    @Bean
    OpenApiCustomizer authErrors() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(op -> {
            boolean isPublic = op.getSecurity() != null && op.getSecurity().isEmpty();
            boolean hasClientError = op.getResponses() != null
                    && op.getResponses().keySet().stream().anyMatch(code -> code.startsWith("4"));
            if (!isPublic && !hasClientError) {
                op.getResponses().addApiResponse("401", new ApiResponse().description("Missing, invalid or expired token"));
                op.getResponses().addApiResponse("403", new ApiResponse().description("The role of the account cannot use this endpoint"));
            }
        }));
    }
}
