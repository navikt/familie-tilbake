package no.nav.tilbakekreving.breeeev

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

data class BegrunnetPeriode(
    val periode: Datoperiode,
    val påkrevdeVurderinger: Set<PåkrevdBegrunnelse>,
)
