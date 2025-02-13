package no.nav.familie.tilbake.api.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.forvaltning.ForvaltningService
import no.nav.familie.tilbake.integration.pdl.internal.logger
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
) {
    @Operation(summary = "Hent korrigert kravgrunnlag")
    @PutMapping(
        path = ["/behandling/{behandlingId}/kravgrunnlag/{eksternKravgrunnlagId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Henter korrigert kravgrunnlag fra økonomi og oppdaterer kravgrunnlag431",
        AuditLoggerEvent.NONE,
        HenteParam.BEHANDLING_ID,
    )
    fun korrigerKravgrunnlag(
        @PathVariable behandlingId: UUID,
        @PathVariable eksternKravgrunnlagId: BigInteger,
    ): Ressurs<String> {
        forvaltningService.korrigerKravgrunnlag(behandlingId, eksternKravgrunnlagId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent korrigert kravgrunnlag")
    @PutMapping(
        path = ["/behandling/{behandlingId}/kravgrunnlag/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Henter korrigert kravgrunnlag fra økonomi og oppdaterer kravgrunnlag431",
        AuditLoggerEvent.NONE,
        HenteParam.BEHANDLING_ID,
    )
    fun korrigerKravgrunnlag(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        forvaltningService.korrigerKravgrunnlag(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Arkiver mottatt kravgrunnlag")
    @PutMapping(
        path = ["/arkiver/kravgrunnlag/{mottattXmlId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Arkiverer mottatt kravgrunnlag",
        AuditLoggerEvent.NONE,
        HenteParam.MOTTATT_XML_ID,
    )
    fun arkiverMottattKravgrunnlag(
        @PathVariable mottattXmlId: UUID,
    ): Ressurs<String> {
        forvaltningService.arkiverMottattKravgrunnlag(mottattXmlId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Tvinghenlegg behandling")
    @PutMapping(
        path = ["/behandling/{behandlingId}/tving-henleggelse/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Tving henlegger behandling",
        AuditLoggerEvent.NONE,
        HenteParam.BEHANDLING_ID,
    )
    fun tvingHenleggBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        forvaltningService.tvingHenleggBehandling(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Flytt behandling tilbake til fakta")
    @PutMapping(
        path = ["/behandling/{behandlingId}/flytt-behandling/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Flytter behandling tilbake til Fakta",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun flyttBehandlingTilFakta(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        forvaltningService.flyttBehandlingsstegTilbakeTilFakta(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Annuler kravgrunnlag")
    @PutMapping(
        path = ["/annuler/kravgrunnlag/{eksternKravgrunnlagId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Annulerer kravgrunnlag",
        AuditLoggerEvent.NONE,
        HenteParam.EKSTERN_KRAVGRUNNLAG_ID,
    )
    fun annulerKravgrunnlag(
        @PathVariable eksternKravgrunnlagId: BigInteger,
    ): Ressurs<String> {
        forvaltningService.annulerKravgrunnlag(eksternKravgrunnlagId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent informasjon som kreves for forvaltning")
    @GetMapping(
        path = ["/ytelsestype/{ytelsestype}/fagsak/{eksternFagsakId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Henter forvaltningsinformasjon",
        AuditLoggerEvent.NONE,
        HenteParam.YTELSESTYPE_OG_EKSTERN_FAGSAK_ID,
    )
    fun hentForvaltningsinfo(
        @PathVariable ytelsestype: Ytelsestype,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<Behandlingsinfo>> = Ressurs.success(forvaltningService.hentForvaltningsinfo(ytelsestype, eksternFagsakId))

    @Operation(summary = "Hent ikke arkiverte kravgrunnlag")
    @GetMapping(
        path = ["/ytelsestype/{ytelsestype}/fagsak/{eksternFagsakId}/ikke-arkivert-kravgrunnlag"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Henter ikke arkiverte kravgrunnlag",
        AuditLoggerEvent.NONE,
        HenteParam.YTELSESTYPE_OG_EKSTERN_FAGSAK_ID,
    )
    fun hentKravgrunnlagsinfo(
        @PathVariable ytelsestype: Ytelsestype,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<Kravgrunnlagsinfo>> = Ressurs.success(forvaltningService.hentIkkeArkiverteKravgrunnlag(ytelsestype, eksternFagsakId))

    @Operation(summary = "Oppretter FinnGammelBehandlingUtenOppgaveTask som logger ut gamle behandlinger uten åpen oppgave")
    @PostMapping(
        path = ["/hentBehandlingerUtenOppgave/fagsystem/{fagsystem}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun finnGamleÅpneBehandlingerUtenOppgave(
        @PathVariable fagsystem: Fagsystem,
    ) {
        oppgaveTaskService.opprettFinnGammelBehandlingUtenOppgaveTask(fagsystem)
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
        @PathVariable fagsystem: Fagsystem,
    ) {
        oppgaveTaskService.finnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave(fagsystem)
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
