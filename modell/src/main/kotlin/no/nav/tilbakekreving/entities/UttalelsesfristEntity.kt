package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Uttalelsesfrist
import java.time.LocalDate
import java.util.UUID

data class UttalelsesfristEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val opprinneligFrist: LocalDate,
    val nyFrist: LocalDate?,
    val begrunnelse: String?,
) {
    fun fraEntity(): Uttalelsesfrist = Uttalelsesfrist(
        id = id,
        opprinneligFrist = opprinneligFrist,
        nyFrist = nyFrist,
        begrunnelse = begrunnelse,
    )
}
