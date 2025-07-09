package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.PåVent
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import java.time.LocalDate

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
