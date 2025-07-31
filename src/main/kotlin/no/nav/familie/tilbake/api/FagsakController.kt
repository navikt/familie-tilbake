package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.klage.FagsystemVedtak
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.api.v1.dto.FagsakDto
import no.nav.tilbakekreving.kontrakter.Behandling
import no.nav.tilbakekreving.kontrakter.FinnesBehandlingResponse
import no.nav.tilbakekreving.kontrakter.KanBehandlingOpprettesManueltRespons
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
    private val fagsakService: FagsakService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @Operation(summary = "Hent fagsak informasjon med bruker og behandlinger")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentFagsak(
        @PathVariable fagsystem: FagsystemDTO,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<FagsakDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(fagsystem, eksternFagsakId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = null,
                minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                auditLoggerEvent = AuditLoggerEvent.ACCESS,
                handling = "Henter fagsak informasjon med bruker og behandlinger",
            )
            return Ressurs.success(tilbakekreving.tilFrontendDto())
        }
        tilgangskontrollService.validerTilgangFagsystemOgFagsakId(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter fagsak informasjon med bruker og behandlinger",
        )
        return Ressurs.success(fagsakService.hentFagsak(Fagsystem.forDTO(fagsystem), eksternFagsakId))
    }

    @Operation(summary = "Sjekk om det finnes en åpen tilbakekrevingsbehandling")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/finnesApenBehandling/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun finnesÅpenTilbakekrevingsbehandling(
        @PathVariable fagsystem: FagsystemDTO,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<FinnesBehandlingResponse> {
        tilgangskontrollService.validerTilgangForFagsystem(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Sjekk om det finnes en åpen tilbakekrevingsbehandling",
        )
        return Ressurs.success(
            fagsakService.finnesÅpenTilbakekrevingsbehandling(
                fagsystem = Fagsystem.forDTO(fagsystem),
                eksternFagsakId = eksternFagsakId,
            ),
        )
    }

    @Operation(summary = "Sjekk om det er mulig å opprette behandling manuelt")
    @GetMapping(
        path = ["/ytelsestype/{ytelsestype}/fagsak/{eksternFagsakId}/kanBehandlingOpprettesManuelt/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun kanBehandlingOpprettesManuelt(
        @PathVariable ytelsestype: Ytelsestype,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<KanBehandlingOpprettesManueltRespons> {
        tilgangskontrollService.validerTilgangYtelsetypeOgFagsakId(
            ytelsestype = ytelsestype,
            eksternFagsakId = eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Sjekk om det er mulig å opprette behandling manuelt",
        )
        return Ressurs.success(fagsakService.kanBehandlingOpprettesManuelt(eksternFagsakId, ytelsestype))
    }

    @Operation(summary = "Hent behandlinger, kalles av fagsystem")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/behandlinger/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentBehandlingerForFagsystem(
        @PathVariable fagsystem: FagsystemDTO,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<Behandling>> {
        tilgangskontrollService.validerTilgangForFagsystem(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter behandlinger for bruk i fagsystem",
        )
        return Ressurs.success(fagsakService.hentBehandlingerForFagsak(Fagsystem.forDTO(fagsystem), eksternFagsakId))
    }

    @Operation(summary = "Hent behandlinger, kalles av fagsystem")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/vedtak/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentVedtakForFagsystem(
        @PathVariable fagsystem: FagsystemDTO,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<FagsystemVedtak>> {
        tilgangskontrollService.validerTilgangForFagsystem(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter behandlinger for bruk i fagsystem",
        )
        return Ressurs.success(fagsakService.hentVedtakForFagsak(Fagsystem.forDTO(fagsystem), eksternFagsakId))
    }
}
