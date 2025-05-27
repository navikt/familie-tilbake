package no.nav.tilbakekreving.entities

import java.time.LocalDate
import java.util.UUID

data class EksternFagsakBehandlingEntity(
    val internId: UUID,
    val eksternId: String,
    val revurderingsresultat: String,
    val revurderingsårsak: String,
    val begrunnelseForTilbakekreving: String,
    val revurderingsvedtaksdato: LocalDate,
)
