package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.api.v1.dto.FristUtsettelseDto
import no.nav.tilbakekreving.entities.FristUtsettelseEntity
import java.time.LocalDate
import java.util.UUID

class UtsettFrist(
    val id: UUID,
    val nyFrist: LocalDate,
    val begrunnelse: String,
) {
    fun tilFrontendDto(): FristUtsettelseDto {
        return FristUtsettelseDto(
            nyFrist = nyFrist,
            begrunnelse = begrunnelse,
        )
    }

    fun tilEntity(behandlingRef: UUID): FristUtsettelseEntity {
        return FristUtsettelseEntity(
            id = id,
            behandlingRef = behandlingRef,
            nyFrist = nyFrist,
            begrunnelse = begrunnelse,
        )
    }
}
