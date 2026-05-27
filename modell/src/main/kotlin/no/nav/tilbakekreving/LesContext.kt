package no.nav.tilbakekreving

import no.nav.tilbakekreving.saksbehandler.Behandler

open class LesContext(
    val behandler: Behandler,
    val features: FeatureToggles,
    val klokke: Klokke,
)
