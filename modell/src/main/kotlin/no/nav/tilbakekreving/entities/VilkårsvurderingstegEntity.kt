package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

data class VilkårsvurderingstegEntity(
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
) {
    fun fraEntity(
        eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
        kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
        foreldelsessteg: Foreldelsesteg,
    ): Vilkårsvurderingsteg {
        return Vilkårsvurderingsteg(
            vurderinger = vurderinger.map { it.fraEntity() },
            eksternFagsakBehandling = eksternFagsakBehandling,
            kravgrunnlagHendelse = kravgrunnlagHendelse,
            foreldelsesteg = foreldelsessteg,
        )
    }
}
