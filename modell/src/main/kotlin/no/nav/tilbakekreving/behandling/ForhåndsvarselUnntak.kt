package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import no.nav.tilbakekreving.entities.ForhåndsvarselUnntakEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselUnntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VarslingsunntakDto
import java.util.UUID

class ForhåndsvarselUnntak(
    private val id: UUID,
    private val begrunnelseForUnntak: BegrunnelseForUnntak,
    private val beskrivelse: String,
) {
    fun skalBeholdeBrukeruttalelse(): Boolean = begrunnelseForUnntak == BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG

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

    fun tilEntity(
        behandlingRef: UUID,
        tilbakeført: ÅrsakTilTilbakeføring?,
    ): ForhåndsvarselUnntakEntity = ForhåndsvarselUnntakEntity(
        id = id,
        behandlingRef = behandlingRef,
        begrunnelseForUnntak = begrunnelseForUnntak,
        beskrivelse = beskrivelse,
        tilbakeført = tilbakeført,
    )
}

enum class BegrunnelseForUnntak {
    IKKE_PRAKTISK_MULIG,
    UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
    ÅPENBART_UNØDVENDIG,
    ALLEREDE_UTTALET_SEG,
}
