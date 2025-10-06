package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class BrevHistorikk(
    private val historikk: MutableList<Brev>,
) : Historikk<UUID, Brev> {
    override fun entry(id: UUID): Brev {
        return historikk.single { it.id == id }
    }

    override fun lagre(innslag: Brev): HistorikkReferanse<UUID, Brev> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.id)
    }

    override fun finn(id: UUID, sporing: Sporing): HistorikkReferanse<UUID, Brev> {
        if (historikk.none { it.id == id }) {
            throw ModellFeil.UgyldigOperasjonException(
                "Fant ikke brev med id: $id i historikken",
                sporing,
            )
        }
        return HistorikkReferanse(this, id)
    }

    override fun nåværende(): HistorikkReferanse<UUID, Brev> {
        return HistorikkReferanse(this, historikk.last().id)
    }

    fun sisteVarselbrev(): Varselbrev? {
        return historikk.filterIsInstance<Varselbrev>().lastOrNull()
    }

    fun tilEntity(): List<BrevEntity> {
        return historikk.map { it.tilEntity() }
    }
}
