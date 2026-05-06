package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.VarslingsUnntak
import no.nav.tilbakekreving.entities.ForhåndsvarselUnntakEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselUnntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VarslingsunntakDto
import java.util.UUID

data class ForhåndsvarselUnntak(
    private val id: UUID,
    private val begrunnelseForUnntak: BegrunnelseForUnntak,
    private val beskrivelse: String,
    private var trengerNyVurdering: Boolean,
) {
    fun trengerNyVurdering(): Boolean = trengerNyVurdering

    fun vurderPåNytt() {
        trengerNyVurdering = true
    }

    fun tilFrontendDto(): ForhåndsvarselUnntakDto = ForhåndsvarselUnntakDto(
        begrunnelseForUnntak = VarslingsUnntak.valueOf(begrunnelseForUnntak.name),
        beskrivelse = beskrivelse,
    )

    internal fun nyTilFrontendDto(): ForhaandsvarselUnntakDto {
        return ForhaandsvarselUnntakDto(
            begrunnelseForUnntak = when (begrunnelseForUnntak) {
                BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG -> VarslingsunntakDto.IKKE_PRAKTISK_MULIG
                BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING -> VarslingsunntakDto.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING
                BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG -> VarslingsunntakDto.ÅPENBART_UNØDVENDIG
                BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG -> VarslingsunntakDto.ALLEREDE_UTTALET_SEG
            },
            beskrivelse = beskrivelse,
        )
    }

    fun tilEntity(behandlingRef: UUID): ForhåndsvarselUnntakEntity = ForhåndsvarselUnntakEntity(
        id = id,
        behandlingRef = behandlingRef,
        begrunnelseForUnntak = begrunnelseForUnntak,
        beskrivelse = beskrivelse,
        trengerNyVurdering = trengerNyVurdering,
    )
}

enum class BegrunnelseForUnntak {
    IKKE_PRAKTISK_MULIG,
    UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
    ÅPENBART_UNØDVENDIG,
    ALLEREDE_UTTALET_SEG,
}
