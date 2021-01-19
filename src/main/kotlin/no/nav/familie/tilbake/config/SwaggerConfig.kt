package no.nav.familie.tilbake.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.ApiKey
import springfox.documentation.service.AuthorizationScope
import springfox.documentation.service.SecurityReference
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.plugins.Docket

@Configuration
class SwaggerConfig {

    private val bearer = "JWT"

    @Bean
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
                              .forPaths(PathSelectors.regex("/api.*"))
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
    }

}
