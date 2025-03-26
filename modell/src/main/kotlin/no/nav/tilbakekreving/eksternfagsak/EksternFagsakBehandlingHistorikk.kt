package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class EksternFagsakBehandlingHistorikk(
    private val historikk: MutableList<EksternFagsakBehandling>,
) : Historikk<UUID, EksternFagsakBehandling> {
    override fun lagre(innslag: EksternFagsakBehandling): HistorikkReferanse<UUID, EksternFagsakBehandling> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.internId)
    }

    override fun finn(id: UUID): EksternFagsakBehandling {
        return historikk.first { it.internId == id }
    }

    override fun nåværende(): HistorikkReferanse<UUID, EksternFagsakBehandling> {
        return HistorikkReferanse(this, historikk.last().internId)
    }
}
