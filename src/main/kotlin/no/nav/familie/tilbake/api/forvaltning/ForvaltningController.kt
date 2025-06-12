package no.nav.familie.tilbake.api.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.forvaltning.ForvaltningService
import no.nav.familie.tilbake.integration.pdl.internal.logger
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// Denne kontrolleren inneholder tjenester som kun brukes av forvaltningsteam via swagger. Frontend skal ikke kalle disse tjenestene.

@RestController
@RequestMapping("/api/forvaltning")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForvaltningController(
    private val forvaltningService: ForvaltningService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val behandlingTilstandService: BehandlingTilstandService,
    private val logService: LogService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @Operation(summary = "Hent korrigert kravgrunnlag")
    @PutMapping(
        path = ["/behandling/{behandlingId}/kravgrunnlag/{eksternKravgrunnlagId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun korrigerKravgrunnlag(
        @PathVariable behandlingId: UUID,
        @PathVariable eksternKravgrunnlagId: BigInteger,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.NONE,
            handling = "Henter korrigert kravgrunnlag fra økonomi og oppdaterer kravgrunnlag431",
        )
        forvaltningService.korrigerKravgrunnlag(behandlingId, eksternKravgrunnlagId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent korrigert kravgrunnlag")
    @PutMapping(
        path = ["/behandling/{behandlingId}/kravgrunnlag/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun korrigerKravgrunnlag(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.NONE,
            handling = "Henter korrigert kravgrunnlag fra økonomi og oppdaterer kravgrunnlag431",
        )
        forvaltningService.korrigerKravgrunnlag(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Arkiver mottatt kravgrunnlag")
    @PutMapping(
        path = ["/arkiver/kravgrunnlag/{mottattXmlId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun arkiverMottattKravgrunnlag(
        @PathVariable mottattXmlId: UUID,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangMottattXMLId(
            mottattXmlId = mottattXmlId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.NONE,
            handling = "Arkiverer mottatt kravgrunnlag",
        )
        forvaltningService.arkiverMottattKravgrunnlag(mottattXmlId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Tvinghenlegg behandling")
    @PutMapping(
        path = ["/behandling/{behandlingId}/tving-henleggelse/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun tvingHenleggBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.NONE,
            handling = "Tving henlegger behandling",
        )
        forvaltningService.tvingHenleggBehandling(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Flytt behandling tilbake til fakta")
    @PutMapping(
        path = ["/behandling/{behandlingId}/flytt-behandling/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun flyttBehandlingTilFakta(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangBehandlingID(
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Flytter behandling tilbake til Fakta",
            )
            tilbakekrevingService.flyttBehandlingsstegTilbakeTilFakta(tilbakekreving)
            return Ressurs.success("OK")
        }

        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Flytter behandling tilbake til Fakta",
        )
        forvaltningService.flyttBehandlingsstegTilbakeTilFakta(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Annuler kravgrunnlag")
    @PutMapping(
        path = ["/annuler/kravgrunnlag/{eksternKravgrunnlagId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun annulerKravgrunnlag(
        @PathVariable eksternKravgrunnlagId: BigInteger,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangKravgrunnlagId(
            eksternKravgrunnlagId = eksternKravgrunnlagId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.NONE,
            handling = "Annulerer kravgrunnlag",
        )
        forvaltningService.annulerKravgrunnlag(eksternKravgrunnlagId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent informasjon som kreves for forvaltning")
    @GetMapping(
        path = ["/ytelsestype/{ytelsestype}/fagsak/{eksternFagsakId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentForvaltningsinfo(
        @PathVariable ytelsestype: Ytelsestype,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<Behandlingsinfo>> {
        tilgangskontrollService.validerTilgangYtelsetypeOgFagsakId(
            ytelsestype = ytelsestype,
            eksternFagsakId = eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.NONE,
            handling = "Henter forvaltningsinformasjon",
        )
        return Ressurs.success(forvaltningService.hentForvaltningsinfo(ytelsestype, eksternFagsakId))
    }

    @Operation(summary = "Hent ikke arkiverte kravgrunnlag")
    @GetMapping(
        path = ["/ytelsestype/{ytelsestype}/fagsak/{eksternFagsakId}/ikke-arkivert-kravgrunnlag"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentKravgrunnlagsinfo(
        @PathVariable ytelsestype: Ytelsestype,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<Kravgrunnlagsinfo>> {
        tilgangskontrollService.validerTilgangYtelsetypeOgFagsakId(
            ytelsestype = ytelsestype,
            eksternFagsakId = eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.NONE,
            handling = "Henter ikke arkiverte kravgrunnlag",
        )
        return Ressurs.success(forvaltningService.hentIkkeArkiverteKravgrunnlag(ytelsestype, eksternFagsakId))
    }

    @Operation(summary = "Oppretter FinnGammelBehandlingUtenOppgaveTask som logger ut gamle behandlinger uten åpen oppgave")
    @PostMapping(
        path = ["/hentBehandlingerUtenOppgave/fagsystem/{fagsystem}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun finnGamleÅpneBehandlingerUtenOppgave(
        @PathVariable fagsystem: FagsystemDTO,
    ) {
        oppgaveTaskService.opprettFinnGammelBehandlingUtenOppgaveTask(Fagsystem.forDTO(fagsystem))
    }

    @Operation(summary = "Manuellt ufører iverksettingssteget uten å sende til oppdrag")
    @PostMapping(
        path = ["/settiverksettingTilUtfort/{taskId}/behandling/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun settIverksettStegTilUtførtOgFortsett(
        @PathVariable taskId: Long,
        @PathVariable behandlingId: UUID,
    ) {
        forvaltningService.hoppOverIverksettingMotOppdrag(behandlingId = behandlingId, taskId = taskId)
    }

    @Operation(summary = "Lag oppdaterOppgaveTask for behandling")
    @PostMapping(
        path = ["/lagOppdaterOppgaveTask"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun lagOppdaterOppgaveTaskForBehandling(
        @RequestBody behandlingIder: List<UUID>,
    ) {
        behandlingIder.forEach { behandlingID ->
            oppgaveTaskService.oppdaterOppgaveTask(
                behandlingId = behandlingID,
                beskrivelse = "Gjenopprettet oppgave",
                frist = LocalDate.now(),
                logContext = logService.contextFraBehandling(behandlingID),
            )
        }
    }

    @Operation(summary = "Send siste tilstand for behandling til DVH")
    @PostMapping(
        path = ["/sendTilstandTilDVH"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun sendSisteTilstandForBehandlingerTilDVH(
        @RequestBody behandlingIder: List<UUID>,
    ) {
        behandlingIder.forEach { behandlingID ->
            behandlingTilstandService.opprettSendingAvBehandlingenManuelt(behandlingId = behandlingID)
        }
    }

    @Operation(summary = "Henter behandlinger med åpen GodkjennVedtak-oppgave eller ingen oppgave, som burde hatt åpen BehandleSak-oppgave")
    @GetMapping(
        path = ["/finnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave/{fagsystem}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun finnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave(
        @PathVariable fagsystem: FagsystemDTO,
    ) {
        oppgaveTaskService.finnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave(Fagsystem.forDTO(fagsystem))
    }

    @Operation(summary = "Ferdigstiller åpen GodkjenneVedtak-oppgave og oppretter BehandleSak-oppgave for behandlinger")
    @PostMapping(
        path = ["/ferdigstillGodkjenneVedtakOppgaveOgOpprettBehandleSakOppgave"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun ferdigstillGodkjenneVedtakOppgaveOgOpprettBehandleSakOppgave(
        @RequestBody behandlingIder: List<UUID>,
    ) {
        behandlingIder.forEach {
            val logContext = logService.contextFraBehandling(it)
            oppgaveTaskService.ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgave(it, "--- Opprettet av familie-tilbake ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n", LocalDate.now(), logContext)
        }
    }

    @Operation(summary = "Ferdigstiller åpen oppgave som skulle vært lukket i en behandling")
    @PostMapping(
        path = ["/ferdigstillOppgaverForBehandling/{behandlingId}/{oppgaveType}"],
    )
    fun ferdigstillOppgaverForBehandling(
        @PathVariable behandlingId: UUID,
        @PathVariable oppgaveType: String,
    ) {
        logger.info("Ferdigstiller oppgave $oppgaveType for behandling $behandlingId")
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId = behandlingId, oppgavetype = oppgaveType)
    }
}

data class Behandlingsinfo(
    val eksternKravgrunnlagId: BigInteger?,
    val kravgrunnlagId: UUID?,
    val kravgrunnlagKravstatuskode: String?,
    val eksternId: String,
    val opprettetTid: LocalDateTime,
    val behandlingId: UUID?,
    val behandlingstatus: Behandlingsstatus?,
)

data class Kravgrunnlagsinfo(
    val eksternKravgrunnlagId: BigInteger,
    val kravgrunnlagKravstatuskode: String,
    val mottattXmlId: UUID?,
    val eksternId: String,
    val opprettetTid: LocalDateTime,
)
