package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.behandling.VergeService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.api.v1.dto.VergeDto
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling/v1/{behandlingId}/verge", produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VergeController(
    private val vergeService: VergeService,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    @Operation(summary = "Opprett verge steg på behandling")
    @PostMapping
    fun opprettVergeSteg(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.CREATE,
            handling = "Oppretter verge steg på behandling",
        )
        vergeService.opprettVergeSteg(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Fjern verge")
    @PutMapping
    fun fjernVerge(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Deaktiverer ev. eksisterende verge.",
        )
        vergeService.fjernVerge(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent verge")
    @GetMapping
    fun hentVerge(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<VergeDto?> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId,
            Behandlerrolle.VEILEDER,
            AuditLoggerEvent.ACCESS,
            "Henter verge informasjon",
        )
        return Ressurs.success(vergeService.hentVerge(behandlingId))
    }
}
