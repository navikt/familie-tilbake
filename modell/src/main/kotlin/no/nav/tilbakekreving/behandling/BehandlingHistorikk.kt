package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto

class BehandlingHistorikk(
    private val historikk: MutableList<Behandling>,
) : FrontendDto<List<BehandlingDto>> {
    override fun tilFrontendDto(): List<BehandlingDto> {
        return historikk.map(Behandling::tilFrontendDto)
    }
}
