package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
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

    override fun finn(id: UUID, sporing: Sporing): HistorikkReferanse<UUID, EksternFagsakBehandling> {
        require(historikk.any { it.internId == id }) {
            throw ModellFeil.UgyldigOperasjonException(
                "Fant ikke ekstern fagsak behandling med historikk-id $id",
                sporing,
            )
        }
        return HistorikkReferanse(this, id)
    }

    override fun entry(id: UUID): EksternFagsakBehandling {
        return historikk.first { it.internId == id }
    }

    override fun nåværende(): HistorikkReferanse<UUID, EksternFagsakBehandling> {
        return HistorikkReferanse(this, historikk.last().internId)
    }

    fun tilEntity(): List<EksternFagsakBehandlingEntity> {
        return historikk.map { it.tilEntity() }
    }
}
