package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.entities.BehandlingHistorikkEntity
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.util.UUID

class BehandlingHistorikk(
    private val historikk: MutableList<Behandling>,
) : Historikk<UUID, Behandling>, FrontendDto<List<BehandlingDto>> {
    override fun finn(id: UUID): Behandling {
        return historikk.single { it.internId == id }
    }

    override fun lagre(innslag: Behandling): HistorikkReferanse<UUID, Behandling> {
        historikk.add(innslag)
        return HistorikkReferanse(this, innslag.internId)
    }

    override fun tilFrontendDto(): List<BehandlingDto> {
        return historikk.map(Behandling::tilFrontendDto)
    }

    override fun nåværende(): HistorikkReferanse<UUID, Behandling> {
        return HistorikkReferanse(this, historikk.last().internId)
    }

    fun tilEntity(): BehandlingHistorikkEntity {
        return BehandlingHistorikkEntity(historikk.map { it.tilEntity() })
    }
}
