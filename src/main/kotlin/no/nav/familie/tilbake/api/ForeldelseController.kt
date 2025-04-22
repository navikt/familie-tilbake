package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
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
class ForeldelseController(
    val foreldelseService: ForeldelseService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @Operation(summary = "Hent foreldelsesinformasjon")
    @GetMapping(
        path = ["{behandlingId}/foreldelse/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentVurdertForeldelse(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<VurdertForeldelseDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                auditLoggerEvent = AuditLoggerEvent.ACCESS,
                handling = "Henter vilkårsvurdering for en gitt behandling",
            )

            return Ressurs.success(tilbakekreving.behandlingHistorikk.finn(behandlingId).foreldelsesteg?.tilFrontendDto() ?: return Ressurs.failure())
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter foreldelsesinformasjon for en gitt behandling",
        )
        return Ressurs.success(foreldelseService.hentVurdertForeldelse(behandlingId))
    }
}
