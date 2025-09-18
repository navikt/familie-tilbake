package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

data class ForeldelsesstegEntity(
    val vurdertePerioder: List<ForeldelseperiodeEntity>,
) {
    fun fraEntity(
        eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
        kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    ): Foreldelsesteg = Foreldelsesteg(
        vurdertePerioder = vurdertePerioder.map { it.fraEntity() },
        eksternFagsakRevurdering = eksternFagsakRevurdering,
        kravgrunnlag = kravgrunnlagHendelse,
    )
}
