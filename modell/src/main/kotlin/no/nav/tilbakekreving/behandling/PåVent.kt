package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.entities.PåVentEntity
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import java.time.LocalDate

class PåVent(
    private val årsak: Venteårsak,
    private val utløpsdato: LocalDate,
    private val begrunnelse: String?,
) {
    fun avventerBruker(): Boolean = årsak == Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING

    fun tilEntity(): PåVentEntity = PåVentEntity(
        årsak = årsak,
        utløpsdato = utløpsdato,
        begrunnelse = begrunnelse,
    )
}
