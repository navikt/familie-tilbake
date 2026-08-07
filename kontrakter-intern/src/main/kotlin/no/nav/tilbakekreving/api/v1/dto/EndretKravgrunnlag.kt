package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

data class EndretKravgrunnlag(
    val gammeltBeløp: Int,
    val nyttBeløp: Int,
    val gammelPeriode: Datoperiode,
    val nyPeriode: Datoperiode,
)
