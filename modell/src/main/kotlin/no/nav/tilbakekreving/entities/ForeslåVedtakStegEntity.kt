package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import java.util.UUID

data class ForeslåVedtakStegEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val vurdert: Boolean,
    val tilbakeført: ÅrsakTilTilbakeføring?,
) {
    fun fraEntity(): ForeslåVedtakSteg {
        return ForeslåVedtakSteg(
            id = id,
            vurdert = vurdert,
            tilbakeført = tilbakeført,
        )
    }
}
