package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg

data class VilkårsvurderingstegEntity(
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
) {
    fun fraEntity(
        foreldelsessteg: Foreldelsesteg,
    ): Vilkårsvurderingsteg {
        return Vilkårsvurderingsteg(
            vurderinger = vurderinger.map { it.fraEntity() },
            foreldelsesteg = foreldelsessteg,
        )
    }
}
