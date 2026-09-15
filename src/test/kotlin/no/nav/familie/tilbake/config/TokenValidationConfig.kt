package no.nav.familie.tilbake.config

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class TokenValidationConfig {
    @Primary
    @Bean
    fun tokenValidationConfigHolder(): TokenValidationContextHolder = SpringTokenValidationContextHolder()
}
