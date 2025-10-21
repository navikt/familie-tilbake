package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import java.util.UUID

data class VilkårsvurderingstegEntity(
    val id: UUID = UUID.randomUUID(),
    val behandlingRef: UUID?,
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
) {
    fun fraEntity(
        foreldelsessteg: Foreldelsesteg,
    ): Vilkårsvurderingsteg {
        return Vilkårsvurderingsteg(
            id = id,
            vurderinger = vurderinger.map { it.fraEntity() },
            foreldelsesteg = foreldelsessteg,
        )
    }
}
