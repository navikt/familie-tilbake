package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg

data class TotrinnsvurderingDto(
    val totrinnsstegsinfo: List<Totrinnsstegsinfo>,
)

data class Totrinnsstegsinfo(
    val behandlingssteg: Behandlingssteg,
    val godkjent: Boolean? = null,
    val begrunnelse: String? = null,
)
