package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.api.v1.dto.FristUtsettelseDto
import no.nav.tilbakekreving.entities.UttalelsesfristEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelsesfristDto
import java.time.LocalDate
import java.util.UUID

class Uttalelsesfrist(
    private val id: UUID,
    private val opprinneligFrist: LocalDate,
    private var nyFrist: LocalDate?,
    private var begrunnelse: String?,
) {
    fun hentFrist(): LocalDate {
        return nyFrist ?: opprinneligFrist
    }

    fun gjeldendeFrist(): LocalDate? = hentFrist().takeIf { it >= LocalDate.now() }

    fun tilFrontendDto(): FristUtsettelseDto {
        return FristUtsettelseDto(
            nyFrist = nyFrist,
            begrunnelse = begrunnelse,
        )
    }

    internal fun nyTilFrontendDto(): UttalelsesfristDto {
        return UttalelsesfristDto(
            opprinneligFrist = opprinneligFrist,
            nyFrist = nyFrist,
            begrunnelse = begrunnelse,
        )
    }

    fun utsettFrist(nyFrist: LocalDate, begrunnelse: String) {
        this.nyFrist = nyFrist
        this.begrunnelse = begrunnelse
    }

    fun tilEntity(behandlingRef: UUID): UttalelsesfristEntity {
        return UttalelsesfristEntity(
            id = id,
            behandlingRef = behandlingRef,
            opprinneligFrist = opprinneligFrist,
            nyFrist = nyFrist,
            begrunnelse = begrunnelse,
        )
    }
}
