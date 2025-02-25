package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.NotNull
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangAdvice
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FaktaFeilutbetalingController(
    val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val tilgangAdvice: TilgangAdvice,
) {
    @Operation(summary = "Hent fakta om feilutbetaling")
    @GetMapping(
        path = ["/behandling/{behandlingId}/fakta/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentFaktaomfeilutbetaling(
        @NotNull
        @PathVariable("behandlingId")
        behandlingId: UUID,
    ): Ressurs<FaktaFeilutbetalingDto> {
        tilgangAdvice.validerTilgangBehandlingID(
            behandlingId,
            Behandlerrolle.VEILEDER,
            AuditLoggerEvent.ACCESS,
            "Henter fakta om feilutbetaling for en gitt behandling",
        )
        return Ressurs.success(faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId))
    }

    @Operation(summary = "Hent inaktive fakta om feilutbetalinger")
    @GetMapping(
        path = ["/behandling/{behandlingId}/fakta/inaktiv"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentInaktivFaktaomfeilutbetaling(
        @NotNull
        @PathVariable("behandlingId")
        behandlingId: UUID,
    ): Ressurs<List<FaktaFeilutbetalingDto>> {
        tilgangAdvice.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter inaktive fakta om feilutbetaling for en gitt behandling",
        )
        return Ressurs.success(faktaFeilutbetalingService.hentInaktivFaktaomfeilutbetaling(behandlingId))
    }
}
