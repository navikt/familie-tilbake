package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDateTime

data class BigQueryBehandlingDataDto(
    val behandlingId: String,
    val opprettetDato: LocalDateTime?,
    val periode: Datoperiode?,
    val behandlingstype: String?,
    val ytelse: String?,
    val beløp: Long?,
    val enhetNavn: String?,
    val enhetKode: String?,
    val status: String?,
    val resultat: String?,
)
