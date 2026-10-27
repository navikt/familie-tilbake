package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.entities.HistorikkEntity
import no.nav.tilbakekreving.entities.KravHendelseEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class KravgrunnlagHistorikk(
    private val historikk: MutableList<KravHendelse>,
) : Historikk<UUID, KravHendelse> {
    override fun finn(
        id: UUID,
        sporing: Sporing,
    ): HistorikkReferanse<UUID, KravHendelse> {
        if (historikk.none { it.id == id }) {
            throw ModellFeil.UgyldigOperasjonException(
                "Fant ikke kravgrunnlag-hendelse med historikk-id $id",
                sporing,
            )
        }
        return HistorikkReferanse(this, id)
    }

    override fun entry(id: UUID): KravHendelse {
        return historikk.single { it.id == id }
    }

    override fun <T : KravHendelse> lagre(innslag: T): HistorikkReferanse<UUID, T> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.id) as HistorikkReferanse<UUID, T>
    }

    fun lagreKronologisk(innslag: KravHendelse): Boolean {
        val indeks = historikk.indexOfFirst { it.opprettet > innslag.opprettet }
        if (indeks != -1) {
            historikk.add(indeks, innslag)
            return false
        }
        lagre(innslag)
        return true
    }

    override fun nåværende(): HistorikkReferanse<UUID, KravHendelse> {
        return HistorikkReferanse(this, historikk.last().id)
    }

    fun nåværendeKravgrunnlagReferanse(): HistorikkReferanse<UUID, KravgrunnlagHendelse> {
        return HistorikkReferanse(
            this,
            (
                historikk.filterIsInstance<KravgrunnlagHendelse>().lastOrNull()
                    ?: throw NoSuchElementException("Ingen kravgrunnlag-hendelse er registrert i historikken")
            ).id,
        ) as HistorikkReferanse<UUID, KravgrunnlagHendelse>
    }

    fun tilEntity(tilbakekrevingId: String): HistorikkEntity<UUID, KravHendelseEntity, KravHendelse> {
        return HistorikkEntity(
            historikk.map { it.tilEntity(tilbakekrevingId) },
        )
    }
}
