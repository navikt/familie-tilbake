package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.PåVent
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import java.time.LocalDate
import java.util.UUID

data class PåVentEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val årsak: Venteårsak,
    val utløpsdato: LocalDate,
    val begrunnelse: String?,
) {
    fun fraEntity() = PåVent(
        id = id,
        årsak = årsak,
        utløpsdato = utløpsdato,
        begrunnelse = begrunnelse,
    )
}
