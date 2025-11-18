package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.VarslingsUnntak
import no.nav.tilbakekreving.entities.ForhåndsvarselUnntakEntity
import java.util.UUID

data class ForhåndsvarselUnntak(
    private val id: UUID,
    private val begrunnelseForUnntak: BegrunnelseForUnntak,
    private val beskrivelse: String,
) {
    fun tilFrontendDto(): ForhåndsvarselUnntakDto = ForhåndsvarselUnntakDto(
        null,
        begrunnelseForUnntak = VarslingsUnntak.valueOf(begrunnelseForUnntak.name),
        beskrivelse = beskrivelse,
        uttalelsesdetaljer = null,
    )

    fun tilEntity(behandlingRef: UUID): ForhåndsvarselUnntakEntity = ForhåndsvarselUnntakEntity(
        id = id,
        behandlingRef = behandlingRef,
        begrunnelseForUnntak = begrunnelseForUnntak,
        beskrivelse = beskrivelse,
    )
}

enum class BegrunnelseForUnntak {
    IKKE_PRAKTISK_MULIG,
    UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
    ÅPENBART_UNØDVENDIG,
    ALLEREDE_UTTALET_SEG,
}
