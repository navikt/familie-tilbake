package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak

data class KravgrunnlagForRevurderingBehov(
    val kravgrunnlagId: String,
    val revurderingsårsak: TilbakekrevingRevurderingsårsak,
) : Behov
