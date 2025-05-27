package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.entities.KravgrunnlagHistorikkEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class KravgrunnlagHistorikk(
    private val historikk: MutableList<KravgrunnlagHendelse>,
) : Historikk<UUID, KravgrunnlagHendelse> {
    override fun finn(id: UUID): KravgrunnlagHendelse {
        return historikk.single { it.internId == id }
    }

    override fun lagre(innslag: KravgrunnlagHendelse): HistorikkReferanse<UUID, KravgrunnlagHendelse> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.internId)
    }

    override fun nåværende(): HistorikkReferanse<UUID, KravgrunnlagHendelse> {
        return HistorikkReferanse(this, historikk.last().internId)
    }

    fun tilEntity(): KravgrunnlagHistorikkEntity {
        return KravgrunnlagHistorikkEntity(historikk.map { it.tilEntity() })
    }
}
