package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.entities.PåVentEntity
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import java.time.LocalDate
import java.util.UUID

class PåVent(
    private val id: UUID,
    private val årsak: Venteårsak,
    private val utløpsdato: LocalDate,
    private val begrunnelse: String?,
) {
    fun avventerBruker(): Boolean = årsak == Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING

    fun tilEntity(behandlingRef: UUID): PåVentEntity = PåVentEntity(
        id = id,
        behandlingRef = behandlingRef,
        årsak = årsak,
        utløpsdato = utløpsdato,
        begrunnelse = begrunnelse,
    )
}
