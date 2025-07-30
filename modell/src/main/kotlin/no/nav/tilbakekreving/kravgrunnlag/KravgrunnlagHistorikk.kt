package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.entities.KravgrunnlagHendelseEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class KravgrunnlagHistorikk(
    private val historikk: MutableList<KravgrunnlagHendelse>,
) : Historikk<UUID, KravgrunnlagHendelse> {
    override fun finn(id: UUID): HistorikkReferanse<UUID, KravgrunnlagHendelse> {
        require(historikk.any { it.internId == id })
        return HistorikkReferanse(this, id)
    }

    override fun entry(id: UUID): KravgrunnlagHendelse {
        return historikk.single { it.internId == id }
    }

    override fun lagre(innslag: KravgrunnlagHendelse): HistorikkReferanse<UUID, KravgrunnlagHendelse> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.internId)
    }

    override fun nåværende(): HistorikkReferanse<UUID, KravgrunnlagHendelse> {
        return HistorikkReferanse(this, historikk.last().internId)
    }

    fun tilEntity(): List<KravgrunnlagHendelseEntity> {
        return historikk.map { it.tilEntity() }
    }
}
