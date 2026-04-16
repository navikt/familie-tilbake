package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import java.util.UUID

data class ForeslåVedtakStegEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val vurdert: Boolean,
    val trengerNyVurdering: Boolean,
) {
    fun fraEntity(): ForeslåVedtakSteg {
        return ForeslåVedtakSteg(
            id = id,
            vurdert = vurdert,
            underkjent = trengerNyVurdering,
        )
    }
}
