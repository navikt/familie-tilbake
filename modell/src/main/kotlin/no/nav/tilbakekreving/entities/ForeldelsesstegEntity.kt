package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg

data class ForeldelsesstegEntity(
    val vurdertePerioder: List<ForeldelseperiodeEntity>,
) {
    fun fraEntity(): Foreldelsesteg = Foreldelsesteg(
        vurdertePerioder = vurdertePerioder.map { it.fraEntity() },
    )
}
