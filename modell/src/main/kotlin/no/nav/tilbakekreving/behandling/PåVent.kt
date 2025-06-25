package no.nav.tilbakekreving.behandling

import java.time.LocalDate
import no.nav.tilbakekreving.entities.PåVentEntity
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak

class PåVent(
    private val årsak: Venteårsak,
    private val utløpsdato: LocalDate,
    private val begrunnelse: String?,
) {
    fun tilEntity(): PåVentEntity = PåVentEntity(
        årsak = årsak,
        utløpsdato = utløpsdato,
        begrunnelse = begrunnelse,
    )
}
