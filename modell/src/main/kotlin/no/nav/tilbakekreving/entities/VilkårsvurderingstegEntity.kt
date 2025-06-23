package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

data class VilkårsvurderingstegEntity(
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
    val foreldelsesteg: ForeldelsesstegEntity,
) {
    fun fraEntity(
        kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    ): Vilkårsvurderingsteg {
        return Vilkårsvurderingsteg(
            vurderinger = vurderinger.map { it.fraEntity() },
            kravgrunnlagHendelse = kravgrunnlagHendelse,
            foreldelsesteg = foreldelsesteg.fraEntity(kravgrunnlagHendelse),
        )
    }
}
