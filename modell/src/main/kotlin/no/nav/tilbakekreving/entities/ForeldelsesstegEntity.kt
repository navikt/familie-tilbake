package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

data class ForeldelsesstegEntity(
    val vurdertePerioder: List<ForeldelseperiodeEntity>,
) {
    fun fraEntity(
        eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
        kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    ): Foreldelsesteg = Foreldelsesteg(
        vurdertePerioder = vurdertePerioder.map { it.fraEntity() },
        eksternFagsakBehandling = eksternFagsakBehandling,
        kravgrunnlag = kravgrunnlagHendelse,
    )
}
