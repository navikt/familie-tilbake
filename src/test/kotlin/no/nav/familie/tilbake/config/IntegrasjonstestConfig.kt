package no.nav.familie.tilbake.config

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("integrasjonstest")
@Configuration
class IntegrasjonstestConfig
