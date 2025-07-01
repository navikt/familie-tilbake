package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

data class VilkårsvurderingstegEntity(
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
) {
    fun fraEntity(
        kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
        foreldelsessteg: Foreldelsesteg,
    ): Vilkårsvurderingsteg {
        return Vilkårsvurderingsteg(
            vurderinger = vurderinger.map { it.fraEntity() },
            kravgrunnlagHendelse = kravgrunnlagHendelse,
            foreldelsesteg = foreldelsessteg,
        )
    }
}
