package no.nav.tilbakekreving.config

import no.nav.tilbakekreving.FeatureToggles
import org.springframework.stereotype.Service

@Service
class FeatureService(
    private val applicationProperties: ApplicationProperties,
) {
    val modellFeatures = FeatureToggles(
        applicationProperties.toggles.nyModell,
    )
}
