package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class EksternFagsakBehandlingHistorikk(
    private val historikk: MutableList<EksternFagsakRevurdering>,
) : Historikk<UUID, EksternFagsakRevurdering> {
    override fun lagre(innslag: EksternFagsakRevurdering): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.id)
    }

    override fun finn(id: UUID, sporing: Sporing): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        if (historikk.none { it.id == id }) {
            throw ModellFeil.UgyldigOperasjonException(
                "Fant ikke ekstern fagsak behandling med historikk-id $id",
                sporing,
            )
        }
        return HistorikkReferanse(this, id)
    }

    override fun entry(id: UUID): EksternFagsakRevurdering {
        return historikk.first { it.id == id }
    }

    override fun nåværende(): HistorikkReferanse<UUID, EksternFagsakRevurdering> {
        return HistorikkReferanse(this, historikk.last().id)
    }

    fun tilEntity(): List<EksternFagsakBehandlingEntity> {
        return historikk.map { it.tilEntity() }
    }
}
