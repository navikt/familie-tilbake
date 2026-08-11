package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import java.util.UUID

data class ForeldelsesstegEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val vurdertePerioder: List<ForeldelseperiodeEntity>,
    val tilbakeført: ÅrsakTilTilbakeføring?,
) {
    fun fraEntity(): Foreldelsesteg = Foreldelsesteg(
        id = id,
        vurdertePerioder = vurdertePerioder.sortedBy { it.periode }.map { it.fraEntity() },
        tilbakeført = tilbakeført,
    )
}
