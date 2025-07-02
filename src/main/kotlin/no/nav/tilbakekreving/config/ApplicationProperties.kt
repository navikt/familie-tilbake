package no.nav.tilbakekreving.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tilbakekreving")
data class ApplicationProperties(
    val toggles: Toggles = Toggles(
        nyModellEnabled = false,
    ),
    val kravgrunnlag: List<String> = emptyList(),
    val tilgangsstyring: Tilgangsstyring,
)
