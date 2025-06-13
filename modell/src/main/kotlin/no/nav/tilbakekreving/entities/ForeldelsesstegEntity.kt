package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

data class ForeldelsesstegEntity(
    val vurdertePerioder: List<ForeldelseperiodeEntity>,
    val kravgrunnlagRef: String,
) {
    fun fraEntity(kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>): Foreldelsesteg = Foreldelsesteg(
        vurdertePerioder = vurdertePerioder.map { Foreldelsesteg.Foreldelseperiode.fraEntity(it) },
        kravgrunnlag = kravgrunnlagHendelse,
    )
}
