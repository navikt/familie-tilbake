package no.nav.tilbakekreving.api

import no.nav.kontrakter.frontend.apis.BehandlingApi
import no.nav.kontrakter.frontend.models.FaktaDto
import no.nav.tilbakekreving.TilbakekrevingService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingApiController(private val tilbakekrevingService: TilbakekrevingService) : BehandlingApi {
    override fun fakta(behandlingId: String): ResponseEntity<FaktaDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(UUID.fromString(behandlingId))
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(tilbakekreving.behandlingHistorikk.nåværende().entry.nyFaktastegFrontendDto())
    }
}
