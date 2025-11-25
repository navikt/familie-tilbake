package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.UtsettFrist
import java.time.LocalDate
import java.util.UUID

data class FristUtsettelseEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val nyFrist: LocalDate,
    val begrunnelse: String,
) {
    fun fraEntity(): UtsettFrist = UtsettFrist(
        id = id,
        nyFrist = nyFrist,
        begrunnelse = begrunnelse,
    )
}
