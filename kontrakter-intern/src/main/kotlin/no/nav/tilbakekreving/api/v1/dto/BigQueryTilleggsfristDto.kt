package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

data class BigQueryTilleggsfristDto(
    val behandlingId: String,
    val antallPerioderMedTilleggsfrist: Int,
    val perioder: List<Datoperiode>,
)
