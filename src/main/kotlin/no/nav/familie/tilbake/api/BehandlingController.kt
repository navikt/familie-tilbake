package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.feilHvis
import no.nav.familie.tilbake.forvaltning.ForvaltningService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangService
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingPåVentDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.ByttEnhetDto
import no.nav.tilbakekreving.api.v1.dto.HenleggelsesbrevFritekstDto
import no.nav.tilbakekreving.api.v1.dto.OpprettRevurderingDto
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.OpprettManueltTilbakekrevingRequest
import no.nav.tilbakekreving.kontrakter.OpprettTilbakekrevingRequest
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(
    private val behandlingService: BehandlingService,
    private val stegService: StegService,
    private val forvaltningService: ForvaltningService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val tilgangService: TilgangService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val applicationProperties: ApplicationProperties,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @Operation(summary = "Opprett tilbakekrevingsbehandling automatisk, kan kalles av fagsystem, batch")
    @PostMapping(
        path = ["/v1"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun opprettBehandling(
        @Valid @RequestBody
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangFagsystemOgFagsakId(
            fagsystem = opprettTilbakekrevingRequest.fagsystem,
            eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.CREATE,
            handling = "Oppretter tilbakekreving",
        )
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        return Ressurs.success(behandling.eksternBrukId.toString(), melding = "Behandling er opprettet.")
    }

    @Operation(summary = "Opprett tilbakekrevingsbehandling manuelt")
    @PostMapping(
        path = ["/manuelt/task/v1"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun opprettBehandlingManuellTask(
        @Valid @RequestBody
        opprettManueltTilbakekrevingRequest: OpprettManueltTilbakekrevingRequest,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangYtelsetypeOgFagsakId(
            ytelsestype = Ytelsestype.forDTO(opprettManueltTilbakekrevingRequest.ytelsestype),
            eksternFagsakId = opprettManueltTilbakekrevingRequest.eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.CREATE,
            handling = "Oppretter tilbakekreving manuelt",
        )
        behandlingService.opprettBehandlingManuellTask(opprettManueltTilbakekrevingRequest)
        return Ressurs.success("Manuell opprettelse av tilbakekreving er startet")
    }

    @Operation(summary = "Opprett tilbakekrevingsrevurdering")
    @PostMapping(
        path = ["/revurdering/v1"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun opprettRevurdering(
        @Valid @RequestBody
        opprettRevurderingDto: OpprettRevurderingDto,
    ): Ressurs<String> {
        tilbakekrevingService.hentTilbakekreving(opprettRevurderingDto.originalBehandlingId) {
            throw ModellFeil.UtenforScopeException(
                UtenforScope.Revurdering,
                Sporing("Ukjent", opprettRevurderingDto.originalBehandlingId.toString()),
            )
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = opprettRevurderingDto.originalBehandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.CREATE,
            handling = "Oppretter tilbakekrevingsrevurdering",
        )
        val behandling = behandlingService.opprettRevurdering(opprettRevurderingDto)
        return Ressurs.success(behandling.eksternBrukId.toString(), melding = "Revurdering er opprettet.")
    }

    @Operation(summary = "Hent behandling")
    @GetMapping(
        path = ["/v1/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentBehandling(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<BehandlingDto> {
        if (applicationProperties.toggles.nyModellEnabled) {
            val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            if (tilbakekreving != null) {
                val rolle = tilgangskontrollService.validerTilgangTilbakekreving(
                    tilbakekreving = tilbakekreving,
                    behandlingId = behandlingId,
                    minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                    auditLoggerEvent = AuditLoggerEvent.ACCESS,
                    handling = "Henter tilbakekrevingsbehandling",
                )
                val behandler = ContextService.hentBehandler(SecureLog.Context.fra(tilbakekreving))
                return Ressurs.success(tilbakekreving.frontendDtoForBehandling(behandler, rolle == Behandlerrolle.BESLUTTER))
            }
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter tilbakekrevingsbehandling",
        )
        return Ressurs.success(behandlingService.hentBehandling(behandlingId))
    }

    @Operation(summary = "Utfør behandlingssteg og fortsett behandling til neste steg")
    @PostMapping(
        path = ["{behandlingId}/steg/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun utførBehandlingssteg(
        @PathVariable("behandlingId") behandlingId: UUID,
        @Valid @RequestBody
        behandlingsstegDto: BehandlingsstegDto,
    ): Ressurs<String> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            val logContext = SecureLog.Context.fra(tilbakekreving)
            val saksbehandler = ContextService.hentBehandler(logContext)

            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle =
                    if (behandlingsstegDto is BehandlingsstegFatteVedtaksstegDto) {
                        Behandlerrolle.BESLUTTER
                    } else {
                        Behandlerrolle.SAKSBEHANDLER
                    },
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Utfører behandlingens aktiv steg og fortsetter den til neste steg",
            )

            tilbakekrevingService.utførSteg(saksbehandler, tilbakekreving.id, behandlingsstegDto, logContext)
            return Ressurs.success("OK")
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle =
                if (behandlingsstegDto is BehandlingsstegFatteVedtaksstegDto) {
                    Behandlerrolle.BESLUTTER
                } else {
                    Behandlerrolle.SAKSBEHANDLER
                },
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Utfører behandlingens aktiv steg og fortsetter den til neste steg",
        )
        // Oppdaterer ansvarlig saksbehandler først slik at historikkinnslag får riktig saksbehandler
        // Hvis det feiler noe,bør det rullet tilbake helt siden begge 2 er på samme transaksjon
        if (stegService.kanAnsvarligSaksbehandlerOppdateres(behandlingId, behandlingsstegDto)) {
            behandlingService.oppdaterAnsvarligSaksbehandler(behandlingId)
        }
        val behandling = behandlingService.hentBehandling(behandlingId)
        stegService.håndterSteg(
            behandlingId,
            behandlingsstegDto,
            SecureLog.Context.medBehandling(behandling.eksternFagsakId, behandlingId.toString()),
        )

        return Ressurs.success("OK")
    }

    @Operation(summary = "Sett behandling på vent")
    @PutMapping(
        path = ["{behandlingId}/vent/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun settBehandlingPåVent(
        @PathVariable("behandlingId") behandlingId: UUID,
        @Valid @RequestBody
        behandlingPåVentDto: BehandlingPåVentDto,
    ): Ressurs<String> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Setter saksbehandler behandling på vent eller utvider fristen",
            )
            tilbakekrevingService.settPåVent(tilbakekreving.id, behandlingPåVentDto.venteårsak, behandlingPåVentDto.tidsfrist, behandlingPåVentDto.begrunnelse)
            return Ressurs.success("OK")
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Setter saksbehandler behandling på vent eller utvider fristen",
        )
        behandlingService.settBehandlingPåVent(behandlingId, behandlingPåVentDto)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Ta behandling av vent")
    @PutMapping(
        path = ["{behandlingId}/gjenoppta/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun taBehandlingAvVent(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<String> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Saksbehandler tar behandling av vent etter å motta brukerrespons eller dokumentasjon",
            )
            tilbakekrevingService.taAvVent(tilbakekreving.id)
            return Ressurs.success("OK")
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Saksbehandler tar behandling av vent etter å motta brukerrespons eller dokumentasjon",
        )
        behandlingService.taBehandlingAvvent(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Henlegg behandling")
    @PutMapping(
        path = ["{behandlingId}/henlegg/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun henleggBehandling(
        @PathVariable("behandlingId") behandlingId: UUID,
        @Valid @RequestBody
        henleggelsesbrevFritekstDto: HenleggelsesbrevFritekstDto,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Saksbehandler henlegger behandling",
        )
        behandlingService.henleggBehandling(behandlingId, henleggelsesbrevFritekstDto)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Bytt enhet")
    @PutMapping(
        path = ["{behandlingId}/bytt-enhet/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun byttEnhet(
        @PathVariable("behandlingId") behandlingId: UUID,
        @Valid @RequestBody
        byttEnhetDto: ByttEnhetDto,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Saksbehandler bytter enhet på behandling",
        )

        behandlingService.byttBehandlendeEnhet(behandlingId, byttEnhetDto)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Angre send til beslutter")
    @PutMapping(
        path = ["{behandlingId}/angre-send-til-beslutter"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun angreSendTilBeslutter(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Saksbehandler angrer på send til beslutter og tar behandling tilbake til saksbehandler",
        )
        behandlingService.angreSendTilBeslutter(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Flytt behandling tilbake til fakta")
    @PutMapping(
        path = ["{behandlingId}/flytt-behandling-til-fakta"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun flyttBehandlingTilFakta(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Flytter behandling tilbake til Fakta",
            )
            tilbakekrevingService.flyttBehandlingTilFakta(tilbakekreving.id)
            return Ressurs.success("OK")
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Flytter behandling tilbake til Fakta",
        )
        val behandling = behandlingService.hentBehandling(behandlingId)

        val logContext = SecureLog.Context.medBehandling(behandling.eksternFagsakId, behandling.behandlingId.toString())
        validerKanSetteBehandlingTilbakeTilFakta(behandling, logContext)
        forvaltningService.flyttBehandlingsstegTilbakeTilFakta(behandlingId)
        return Ressurs.success("OK")
    }

    private fun validerKanSetteBehandlingTilbakeTilFakta(
        behandling: BehandlingDto,
        logContext: SecureLog.Context,
    ) {
        feilHvis(
            !erAnsvarligSaksbehandler(
                behandling,
                logContext,
            ) &&
                !tilgangService.harInnloggetBrukerForvalterRolle(),
            HttpStatus.FORBIDDEN,
            logContext,
        ) {
            "Kun ansvarlig saksbehandler kan flytte behandling tilbake til fakta"
        }
        feilHvis(
            behandlingskontrollService.erBehandlingPåVent(behandling.behandlingId),
            HttpStatus.FORBIDDEN,
            logContext,
        ) {
            "Behandling er på vent og kan derfor ikke flyttes tilbake til fakta"
        }
        val behandlingstatus = behandlingService.hentBehandling(behandling.behandlingId).status
        feilHvis(behandlingstatus != Behandlingsstatus.UTREDES, HttpStatus.FORBIDDEN, logContext) {
            "Behandling er ikke under utredning, og kan derfor ikke flyttes tilbake til fakta"
        }
    }

    private fun erAnsvarligSaksbehandler(
        behandling: BehandlingDto,
        logContext: SecureLog.Context,
    ): Boolean = ContextService.hentSaksbehandler(logContext) == behandling.ansvarligSaksbehandler
}
