package no.nav.familie.tilbake.api

import no.nav.familie.tilbake.config.ApplicationProperties
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.tilDto
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.v2.TilbakekrevingService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.api.v1.dto.HistorikkinnslagDto
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class HistorikkController(
    private val historikkService: HistorikkService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val applicationProperties: ApplicationProperties,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @GetMapping(
        "/{behandlingId}/historikk",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentHistorikkinnslag(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<List<HistorikkinnslagDto?>> {
        if (applicationProperties.toggles.nyModellEnabled) {
            val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            if (tilbakekreving != null) {
                tilgangskontrollService.validerTilgangTilbakekreving(
                    tilbakekreving = tilbakekreving,
                    behandlingId = behandlingId,
                    minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                    auditLoggerEvent = AuditLoggerEvent.ACCESS,
                    handling = "Henter tilbakekrevingsbehandling",
                )
                return Ressurs.success(emptyList())
            }
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter historikkinnslag",
        )
        val historikkInnslagDtoSortertEtterOpprettetTidspunkt =
            historikkService
                .hentHistorikkinnslag(behandlingId)
                .map { it.tilDto() }
                .sortedBy { it.opprettetTid }

        return Ressurs.success(historikkInnslagDtoSortertEtterOpprettetTidspunkt)
    }
}
