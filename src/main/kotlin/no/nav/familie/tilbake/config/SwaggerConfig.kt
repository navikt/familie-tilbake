package no.nav.familie.tilbake.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig(@Value("\${AUTHORIZATION_URL}")
                     val authorizationUrl: String,
                    @Value("\${TOKEN_URL}")
                    val tokenUrl: String,
                    @Value("\${API_SCOPE}")
                    val apiScope: String) {

    @Bean
    fun tilbakekrevingApi(): OpenAPI {
        return OpenAPI().info(Info().title("Tilbakekreving API").version("v1"))
                .components(Components().addSecuritySchemes("oauth2", securitySchemes()))
                .addSecurityItem(SecurityRequirement().addList("oauth2", listOf("read", "write")))
    }

    private fun securitySchemes(): SecurityScheme {
        return SecurityScheme()
                .name("oauth2")
                .type(SecurityScheme.Type.OAUTH2)
                .scheme("oauth2")
                .`in`(SecurityScheme.In.HEADER)
                .flows(OAuthFlows()
                               .authorizationCode(OAuthFlow().authorizationUrl(authorizationUrl)
                                                          .tokenUrl(tokenUrl)
                                                          .scopes(Scopes().addString(apiScope,"read,write"))))

    }

    /*@Bean
    fun oppdragApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .securitySchemes(securitySchemes())
            .securityContexts(securityContext())
            .apiInfo(apiInfo()).select()
            .apis(RequestHandlerSelectors.basePackage("no.nav.familie.tilbake"))
            .paths(PathSelectors.any())
            .build()
    }

    private fun securitySchemes(): List<ApiKey> {
        return listOf(ApiKey(bearer, "Authorization", "header"))
    }

    private fun securityContext(): List<SecurityContext> {
        return listOf(SecurityContext.builder()
                          .securityReferences(defaultAuth())
                          .operationSelector {
                              it.requestMappingPattern().startsWith("/api")
                          }
                          .build())
    }

    private fun defaultAuth(): List<SecurityReference> {
        val authorizationScope = AuthorizationScope("global", "accessEverything")
        val authorizationScopes = arrayOfNulls<AuthorizationScope>(1)
        authorizationScopes[0] = authorizationScope
        return listOf(SecurityReference("JWT", authorizationScopes))
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfoBuilder().build()
    }*/

}
