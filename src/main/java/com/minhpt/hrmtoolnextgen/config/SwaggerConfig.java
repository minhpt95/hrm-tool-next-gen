package com.minhpt.hrmtoolnextgen.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;

import com.minhpt.hrmtoolnextgen.constant.ApiConstant;

import java.util.List;


@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    private final Environment env;

    @Bean
    public OpenAPI hrmToolOpenAPI() {

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securitySchemeName)
                )
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                )
                .info(
                        new Info()
                                .title("HRM Internal Backend")
                                .description("HRM Tool for HRM Web")
                                .version(env.getProperty("build.version", "v1.0.0"))
                                .license(
                                        new License()
                                                .name("OpenApi v1.7.0")
                                                .url("https://springdoc.org")
                                )
                )
                .externalDocs(
                        new ExternalDocumentation()
                                .description("")
                                .url(""))
                ;
    }

    @Bean
    public OperationCustomizer acceptLanguageHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            Parameter acceptLanguageParam = new Parameter()
                    .in("header")
                    .name("Accept-Language")
                    .description("Language preference (en, vi)")
                    .required(false)
                    .schema(new StringSchema()
                            ._enum(List.of("en", "vi"))
                            ._default("en"));
            operation.addParametersItem(acceptLanguageParam);
            return operation;
        };
    }

        @Bean
        public OpenApiCustomizer deprecatedLegacyApiCustomizer() {
                return openApi -> {
                        if (openApi.getPaths() == null) {
                                return;
                        }
                        openApi.getPaths().forEach((path, pathItem) -> {
                                if (!ApiConstant.isLegacyPath(path)) {
                                        return;
                                }

                                String successorPath = ApiConstant.toVersionedPath(path);
                                pathItem.readOperations().forEach(operation -> {
                                        operation.setDeprecated(true);
                                        String note = String.format(
                                                        "Deprecated legacy endpoint. Use %s instead. Sunset: %s.",
                                                        successorPath,
                                                        ApiConstant.LEGACY_API_SUNSET
                                        );
                                        String description = operation.getDescription();
                                        operation.setDescription(description == null || description.isBlank()
                                                        ? note
                                                        : description + "\n\n" + note);
                                });
                        });
                };
        }
}
