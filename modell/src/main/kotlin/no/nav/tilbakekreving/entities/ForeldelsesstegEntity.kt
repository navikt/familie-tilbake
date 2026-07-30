package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import java.util.UUID

data class ForeldelsesstegEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val vurdertePerioder: List<ForeldelseperiodeEntity>,
    val trengerNyVurdering: Boolean,
) {
    fun fraEntity(): Foreldelsesteg = Foreldelsesteg(
        id = id,
        vurdertePerioder = vurdertePerioder.sortedBy { it.periode }.map { it.fraEntity() },
        underkjent = trengerNyVurdering,
    )
}
