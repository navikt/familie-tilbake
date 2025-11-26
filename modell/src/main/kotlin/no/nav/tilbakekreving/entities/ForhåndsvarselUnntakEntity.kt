package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.behandling.ForhåndsvarselUnntak
import java.util.UUID

data class ForhåndsvarselUnntakEntity(
    val id: UUID = UUID.randomUUID(),
    val behandlingRef: UUID,
    val begrunnelseForUnntak: BegrunnelseForUnntak,
    val beskrivelse: String,
) {
    fun fraEntity(): ForhåndsvarselUnntak {
        return ForhåndsvarselUnntak(
            id = id,
            begrunnelseForUnntak = begrunnelseForUnntak,
            beskrivelse = beskrivelse,
        )
    }
}
