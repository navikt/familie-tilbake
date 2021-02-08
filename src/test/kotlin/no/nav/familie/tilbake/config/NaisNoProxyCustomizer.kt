package no.nav.familie.tilbake.config

import no.nav.familie.http.config.INaisProxyCustomizer
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Skal ikke bruke proxy for lokal kjøring eller tester, blir brukt i RestTemplateBuilderBean
 */
@Component
@Primary
class NaisNoProxyCustomizer : INaisProxyCustomizer {

    override fun customize(restTemplate: RestTemplate?) {
    }
}
