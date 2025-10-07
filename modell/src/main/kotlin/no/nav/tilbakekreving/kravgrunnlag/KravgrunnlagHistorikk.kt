package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.entities.KravgrunnlagHendelseEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class KravgrunnlagHistorikk(
    private val historikk: MutableList<KravgrunnlagHendelse>,
) : Historikk<UUID, KravgrunnlagHendelse> {
    override fun finn(
        id: UUID,
        sporing: Sporing,
    ): HistorikkReferanse<UUID, KravgrunnlagHendelse> {
        if (historikk.none { it.id == id }) {
            throw ModellFeil.UgyldigOperasjonException(
                "Fant ikke kravgrunnlag-hendelse med historikk-id $id",
                sporing,
            )
        }
        return HistorikkReferanse(this, id)
    }

    override fun entry(id: UUID): KravgrunnlagHendelse {
        return historikk.single { it.id == id }
    }

    override fun lagre(innslag: KravgrunnlagHendelse): HistorikkReferanse<UUID, KravgrunnlagHendelse> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.id)
    }

    override fun nåværende(): HistorikkReferanse<UUID, KravgrunnlagHendelse> {
        return HistorikkReferanse(this, historikk.last().id)
    }

    fun tilEntity(tilbakekrevingId: String): List<KravgrunnlagHendelseEntity> {
        return historikk.map { it.tilEntity(tilbakekrevingId) }
    }
}
