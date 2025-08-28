package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.api.v1.dto.BehandlingsoppsummeringDto
import no.nav.tilbakekreving.entities.BehandlingEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class BehandlingHistorikk(
    private val historikk: MutableList<Behandling>,
) : Historikk<UUID, Behandling> {
    override fun finn(id: UUID, sporing: Sporing): HistorikkReferanse<UUID, Behandling> {
        if (historikk.none { it.internId == id }) {
            throw ModellFeil.UgyldigOperasjonException(
                "Fant ikke behandling med historikk-id $id",
                sporing,
            )
        }
        return HistorikkReferanse(this, id)
    }

    override fun entry(id: UUID): Behandling {
        return historikk.single { it.internId == id }
    }

    override fun lagre(innslag: Behandling): HistorikkReferanse<UUID, Behandling> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.internId)
    }

    fun tilOppsummeringDto(): List<BehandlingsoppsummeringDto> {
        return historikk.map(Behandling::tilOppsummeringDto)
    }

    override fun nåværende(): HistorikkReferanse<UUID, Behandling> {
        return HistorikkReferanse(this, historikk.last().internId)
    }

    fun forrige(): HistorikkReferanse<UUID, Behandling>? {
        return historikk.dropLast(1).lastOrNull()?.let { HistorikkReferanse(this, it.internId) }
    }

    fun tilEntity(): List<BehandlingEntity> {
        return historikk.map { it.tilEntity() }
    }
}
