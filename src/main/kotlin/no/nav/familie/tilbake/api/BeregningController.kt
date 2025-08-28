package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling/")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(
    val tilbakekrevingsberegningService: TilbakekrevingsberegningService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @Operation(summary = "Hent beregningsresultat")
    @GetMapping(
        path = ["{behandlingId}/beregn/resultat/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentBeregningsresultat(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<BeregningsresultatDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                auditLoggerEvent = AuditLoggerEvent.ACCESS,
                handling = "Henter beregningsresultat",
            )
            return Ressurs.success(tilbakekreving.behandlingHistorikk.nåværende().entry.beregnForFrontend())
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter beregningsresultat",
        )
        return Ressurs.success(tilbakekrevingsberegningService.hentBeregningsresultat(behandlingId))
    }
}
