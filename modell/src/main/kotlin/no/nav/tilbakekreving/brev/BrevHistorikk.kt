package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class BrevHistorikk(
    private val historikk: MutableList<Brev>,
) : Historikk<UUID, Brev> {
    override fun finn(id: UUID): Brev {
        return historikk.single { it.internId == id }
    }

    override fun lagre(innslag: Brev): HistorikkReferanse<UUID, Brev> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.internId)
    }

    override fun nåværende(): HistorikkReferanse<UUID, Brev> {
        return HistorikkReferanse(this, historikk.last().internId)
    }

    fun sisteVarselbrev(): Varselbrev? {
        return historikk.filterIsInstance<Varselbrev>().lastOrNull()
    }

    fun tilEntity(): List<BrevEntity> {
        return historikk.map { it.tilEntity() }
    }
}
