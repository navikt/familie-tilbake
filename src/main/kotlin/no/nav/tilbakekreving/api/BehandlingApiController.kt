package no.nav.tilbakekreving.api

import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.kontrakter.frontend.apis.BehandlingApi
import no.nav.kontrakter.frontend.models.FaktaOmFeilutbetalingDto
import no.nav.kontrakter.frontend.models.OppdaterFaktaOmFeilutbetalingDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ProtectedWithClaims(issuer = "azuread")
class BehandlingApiController(
    private val tilbakekrevingService: TilbakekrevingService,
    private val tilgangskontrollService: TilgangskontrollService,
) : BehandlingApi {
    override fun fakta(behandlingId: String): ResponseEntity<FaktaOmFeilutbetalingDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(UUID.fromString(behandlingId))
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = UUID.fromString(behandlingId),
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter fakta om feilutbetalingen",
        )
        return ResponseEntity.ok(tilbakekreving.tilFeilutbetalingFrontendDto())
    }

    override fun oppdaterFakta(behandlingId: String, oppdaterFaktaOmFeilutbetalingDto: OppdaterFaktaOmFeilutbetalingDto): ResponseEntity<FaktaOmFeilutbetalingDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(UUID.fromString(behandlingId))
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = UUID.fromString(behandlingId),
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Henter fakta om feilutbetalingen",
        )
        return tilbakekrevingService.hentTilbakekreving(UUID.fromString(behandlingId)) {
            if (oppdaterFaktaOmFeilutbetalingDto.vurdering?.oppdaget != null) {
                it.behandlingHistorikk.nåværende().entry.vurder(oppdaterFaktaOmFeilutbetalingDto.vurdering!!.oppdaget!!)
            }
            if (oppdaterFaktaOmFeilutbetalingDto.vurdering?.årsak != null) {
                it.behandlingHistorikk.nåværende().entry.vurder(oppdaterFaktaOmFeilutbetalingDto.vurdering!!.årsak!!)
            }
            if (oppdaterFaktaOmFeilutbetalingDto.perioder != null) {
                it.behandlingHistorikk.nåværende().entry.vurderFaktaPerioder(oppdaterFaktaOmFeilutbetalingDto.perioder!!)
            }
            ResponseEntity.ok(it.tilFeilutbetalingFrontendDto())
        } ?: ResponseEntity.notFound().build()
    }
}
