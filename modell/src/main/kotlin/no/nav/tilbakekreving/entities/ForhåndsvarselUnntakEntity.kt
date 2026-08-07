package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.behandling.ForhåndsvarselUnntak
import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import java.util.UUID

data class ForhåndsvarselUnntakEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val begrunnelseForUnntak: BegrunnelseForUnntak,
    val beskrivelse: String,
    val tilbakeført: ÅrsakTilTilbakeføring?,
) {
    fun fraEntity(): ForhåndsvarselUnntak {
        return ForhåndsvarselUnntak(
            id = id,
            begrunnelseForUnntak = begrunnelseForUnntak,
            beskrivelse = beskrivelse,
            tilbakeført = tilbakeført,
        )
    }
}
