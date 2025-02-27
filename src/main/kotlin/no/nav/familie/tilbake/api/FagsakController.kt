package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.kontrakter.Fagsystem
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.klage.FagsystemVedtak
import no.nav.familie.tilbake.kontrakter.tilbakekreving.Behandling
import no.nav.familie.tilbake.kontrakter.tilbakekreving.FinnesBehandlingResponse
import no.nav.familie.tilbake.kontrakter.tilbakekreving.KanBehandlingOpprettesManueltRespons
import no.nav.familie.tilbake.kontrakter.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.familie.tilbake.sikkerhet.TilgangAdvice
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
    private val tilgangAdvice: TilgangAdvice,
) {
    @Operation(summary = "Hent fagsak informasjon med bruker og behandlinger")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.VEILEDER,
        "Henter fagsak informasjon med bruker og behandlinger",
        AuditLoggerEvent.ACCESS,
        HenteParam.FAGSYSTEM_OG_EKSTERN_FAGSAK_ID,
    )
    fun hentFagsak(
        @PathVariable fagsystem: Fagsystem,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<FagsakDto> = Ressurs.success(fagsakService.hentFagsak(fagsystem, eksternFagsakId))

    @Operation(summary = "Sjekk om det finnes en åpen tilbakekrevingsbehandling")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/finnesApenBehandling/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.VEILEDER,
        "Sjekk om det finnes en åpen tilbakekrevingsbehandling",
        AuditLoggerEvent.ACCESS,
        HenteParam.FAGSYSTEM_OG_EKSTERN_FAGSAK_ID,
    )
    fun finnesÅpenTilbakekrevingsbehandling(
        @PathVariable fagsystem: Fagsystem,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<FinnesBehandlingResponse> =
        Ressurs.success(
            fagsakService.finnesÅpenTilbakekrevingsbehandling(
                fagsystem = fagsystem,
                eksternFagsakId = eksternFagsakId,
            ),
        )

    @Operation(summary = "Sjekk om det er mulig å opprette behandling manuelt")
    @GetMapping(
        path = ["/ytelsestype/{ytelsestype}/fagsak/{eksternFagsakId}/kanBehandlingOpprettesManuelt/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun kanBehandlingOpprettesManuelt(
        @PathVariable ytelsestype: Ytelsestype,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<KanBehandlingOpprettesManueltRespons> {
        tilgangAdvice.validerTilgangYtelsetypeOgFagsakId(
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
    @Rolletilgangssjekk(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        handling = "Henter behandlinger for bruk i fagsystem",
        AuditLoggerEvent.ACCESS,
        henteParam = HenteParam.FAGSYSTEM_OG_EKSTERN_FAGSAK_ID,
    )
    fun hentBehandlingerForFagsystem(
        @PathVariable fagsystem: Fagsystem,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<Behandling>> = Ressurs.success(fagsakService.hentBehandlingerForFagsak(fagsystem, eksternFagsakId))

    @Operation(summary = "Hent behandlinger, kalles av fagsystem")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/vedtak/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        minimumBehandlerrolle = Behandlerrolle.VEILEDER,
        handling = "Henter behandlinger for bruk i fagsystem",
        AuditLoggerEvent.ACCESS,
        henteParam = HenteParam.FAGSYSTEM_OG_EKSTERN_FAGSAK_ID,
    )
    fun hentVedtakForFagsystem(
        @PathVariable fagsystem: Fagsystem,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<FagsystemVedtak>> = Ressurs.success(fagsakService.hentVedtakForFagsak(fagsystem, eksternFagsakId))
}
