package no.nav.familie.tilbake.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tilbakekreving")
data class ApplicationProperties(
    val toggles: Toggles = Toggles(
        nyModellEnabled = false,
    ),
    val kravgrunnlag: List<String> = emptyList(),
) {
    data class Toggles(
        val nyModellEnabled: Boolean,
    )
}
