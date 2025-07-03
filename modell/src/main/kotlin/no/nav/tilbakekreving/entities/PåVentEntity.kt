package no.nav.tilbakekreving.entities

import java.time.LocalDate
import no.nav.tilbakekreving.behandling.PåVent
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak

data class PåVentEntity(
    val årsak: Venteårsak,
    val utløpsdato: LocalDate,
    val begrunnelse: String?,
) {
    fun fraEntity() = PåVent(
        årsak = årsak,
        utløpsdato = utløpsdato,
        begrunnelse = begrunnelse,
    )
}
