package no.nav.familie.tilbake.api.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Institusjon
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.forvaltning.ForvaltningService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattArkivRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangService
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.BehovMediator
import no.nav.tilbakekreving.FagsystemUtil
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import no.nav.tilbakekreving.kravgrunnlag.StatusmeldingBufferRepository
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
    private val applicationProperties: ApplicationProperties,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val fagsakRepository: FagsakRepository,
    private val behovMediator: BehovMediator,
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val statusmeldingBufferRepository: StatusmeldingBufferRepository,
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
    private val økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository,
    private val tilgangService: TilgangService,
) {
    private val logger = TracedLogger.getLogger<ForvaltningController>()

    @Operation(summary = "Hent behandlings-ID-er med behandlingsårsak REVURDERING_ANNEN_ÅRSAK")
    @GetMapping(
        path = ["/behandlingsarsak/revurdering-annen-arsak/behandlinger"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentBehandlingsIderMedRevurderingAnnenÅrsak(): Ressurs<List<UUID>> {
        if (!tilgangService.harInnloggetBrukerForvalterRolle()) {
            throw ForbiddenError(
                message = "Mangler forvaltertilgang til å hente behandlings-ID-er med revurdering annen årsak",
                frontendFeilmelding = "Du har ikke tilgang til å hente behandlings-ID-er med revurdering annen årsak",
                logContext = SecureLog.Context.tom(),
            )
        }
        return Ressurs.success(behandlingRepository.hentBehandlingsIderMedRevurderingAnnenÅrsak())
    }

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
        val response = tilbakekrevingService.endreTilbakekreving(behandlingId, ValideringContext.ForvaltningHentKorrigertKravgrunnlag) { tilbakekreving, context ->
            val behov = tilbakekreving.trengerKorrigertKravgrunnlag()
            val korrigertKravgrunnlag = behovMediator.hentKorrigertKravgrunnlag(behov)
            tilbakekreving.håndter(korrigertKravgrunnlag, context)
            Ressurs.success("OK")
        }
        if (response != null) return response
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
        val response = tilbakekrevingService.endreTilbakekreving(behandlingId, ValideringContext.ForvaltningFlyttTilFakta) { tilbakekreving, context ->
            tilbakekreving.gjørSaksbehandling(behandlingId, context) {
                flyttTilbakeTilFakta()
            }
            Ressurs.success("OK")
        }
        if (response != null) {
            return response
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
        @PathVariable ytelsestype: YtelsestypeDTO,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<Behandlingsinfo>> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(TilbakekrevingFilter.fagsakEllerTilbakekrevingId(eksternFagsakId, FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype)), ValideringContext.HentForvaltningsinfo)
        if (tilbakekreving != null) {
            return Ressurs.success(
                listOf(
                    tilbakekreving.hentBehandlingsinformasjon().let {
                        Behandlingsinfo(
                            eksternKravgrunnlagId = null,
                            kravgrunnlagId = null,
                            kravgrunnlagKravstatuskode = null,
                            eksternId = it.kravgrunnlagReferanse,
                            opprettetTid = it.opprettetTid,
                            behandlingId = it.behandlingId,
                            behandlingstatus = null,
                        )
                    },
                ),
            )
        }
        tilgangskontrollService.validerTilgangYtelsetypeOgFagsakId(
            ytelsestype = Ytelsestype.forDTO(ytelsestype),
            eksternFagsakId = eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.NONE,
            handling = "Henter forvaltningsinformasjon",
        )
        return Ressurs.success(forvaltningService.hentForvaltningsinfo(Ytelsestype.forDTO(ytelsestype), eksternFagsakId))
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

    @Operation(summary = "Hent mottatte og arkiverte meldinger fra økonomi i gammel modell")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/mottatte-meldinger"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentMottatteMeldinger(
        @PathVariable fagsystem: FagsystemDTO,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<MottatteMeldinger> {
        tilgangskontrollService.validerTilgangFagsystemOgFagsakId(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.NONE,
            handling = "Henter mottatte og arkiverte meldinger fra økonomi",
        )
        return Ressurs.success(
            MottatteMeldinger(
                mottatte = økonomiXmlMottattRepository.findByEksternFagsakId(eksternFagsakId).map { xml ->
                    MottattMelding(
                        mottattXmlId = xml.id,
                        ytelsestype = xml.ytelsestype,
                        eksternKravgrunnlagId = xml.eksternKravgrunnlagId,
                        vedtakId = xml.vedtakId,
                        kravstatuskode = xml.kravstatuskode.navn,
                        referanse = xml.referanse,
                        sperret = xml.sperret,
                        opprettetTid = xml.sporbar.opprettetTid,
                        melding = xml.melding,
                    )
                },
                arkiverte = økonomiXmlMottattArkivRepository.findByEksternFagsakId(eksternFagsakId).map { arkiv ->
                    ArkivertMelding(
                        arkivId = arkiv.id,
                        ytelsestype = arkiv.ytelsestype,
                        gammelMottattXmlId = arkiv.gammel_okonomi_xml_mottatt_id,
                        opprettetTid = arkiv.sporbar.opprettetTid,
                        melding = arkiv.melding,
                    )
                },
            ),
        )
    }

    @Operation(summary = "Hent kravgrunnlag og statusmeldinger fra bufferet i ny modell")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/buffer-meldinger"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentBufferMeldinger(
        @PathVariable fagsystem: FagsystemDTO,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<BufferMeldinger> {
        tilbakekrevingService.lesTilbakekreving(
            TilbakekrevingFilter.fagsak(eksternFagsakId, fagsystem),
            ValideringContext.ForvaltningHentBufferMeldinger,
            validerScope = false,
        ) ?: throw Feil(
            message = "Fant ingen tilbakekreving for fagsystem=$fagsystem og eksternFagsakId=$eksternFagsakId",
            logContext = SecureLog.Context.utenBehandling(eksternFagsakId),
            httpStatus = HttpStatus.NOT_FOUND,
        )
        return Ressurs.success(
            BufferMeldinger(
                kravgrunnlag = kravgrunnlagBufferRepository.hentForFagsystemId(eksternFagsakId).map {
                    BufferKravgrunnlag(
                        kravgrunnlagId = it.entity.kravgrunnlagId,
                        lest = it.lest,
                        mottatt = it.entity.mottatt,
                        melding = it.entity.kravgrunnlag,
                    )
                },
                statusmeldinger = statusmeldingBufferRepository.hentForFagsystemId(eksternFagsakId).map {
                    BufferStatusmelding(
                        id = it.entity.id,
                        vedtakId = it.entity.vedtakId,
                        status = it.entity.status,
                        lest = it.lest,
                        mottatt = it.mottatt,
                        melding = it.entity.statusmelding,
                    )
                },
            ),
        )
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
        logger.medContext(SecureLog.Context.medBehandling(null, behandlingId.toString())) {
            info("Ferdigstiller oppgave $oppgaveType for behandling $behandlingId")
        }
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId = behandlingId, oppgavetype = oppgaveType)
    }

    @Operation(summary = "Tvinger en migrering av alle saker i ny modell")
    @PostMapping("/migrer-alle-saker")
    fun migrerAlleSaker() {
        tilbakekrevingRepository.hentAlleTilbakekrevinger()?.forEach { entity ->
            tilbakekrevingRepository.hentOgLagreResultat(TilbakekrevingFilter.tilbakekreving(entity.id)) { it, _ ->
                val tilbakekreving = it.fraEntity()
                logger.medContext(SecureLog.Context.fra(tilbakekreving)) {
                    info("Migrerer sak {}", tilbakekreving.hentTilbakekrevingUrl(applicationProperties.frontendUrl))
                }
                tilbakekreving.tilEntity()
            }
        }
    }

    @Operation(summary = "Kjør en påminnelse for alle saker i tilstand selv om de ikke er ")
    @PostMapping("/poke")
    fun sendPåminnelseTilAlleSakerITilstand(tilstand: TilbakekrevingTilstand) {
        tilbakekrevingRepository.oppdaterNestePåminnelse(tilstand)
    }

    @Operation(summary = "Tving oppdatering av fagsysteminfo")
    @PostMapping("/oppdater-fagsysteminfo/{behandlingId}")
    fun oppdaterFagsysteminfo(
        @PathVariable behandlingId: UUID,
    ) {
        tilbakekrevingService.endreTilbakekreving(behandlingId, ValideringContext.ForvaltningOppdaterFagsysteminfo) { tilbakekreving, context ->
            tilbakekreving.trengerFagsysteminfo(context)
        }
    }

    @Operation(summary = "Henter hele JSON-objektet for en sak")
    @PostMapping("/dump/{behandlingId}")
    fun dumpFagsak(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<TilbakekrevingEntity> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(TilbakekrevingFilter.behandling(behandlingId), ValideringContext.ForvaltningDumpFagsak, validerScope = false)
        return ResponseEntity.ofNullable(tilbakekreving?.tilEntity())
    }

    @Operation(summary = "Henter hele JSON-objektet for en sak")
    @PostMapping("/dump/{fagsystem}/{fagsystemId}")
    fun dumpFagsakFagsystemId(
        @PathVariable fagsystem: FagsystemDTO,
        @PathVariable fagsystemId: String,
    ): ResponseEntity<TilbakekrevingEntity> {
        tilgangskontrollService.validerTilgangTilbakekreving(
            fagsystem = fagsystem,
            fagsystemId = fagsystemId,
            ValideringContext.ForvaltningDumpFagsak,
            brukerIdent = null,
            ContextService.hentBehandler(SecureLog.Context.utenBehandling(fagsystemId)),
        )
        return ResponseEntity.ofNullable(
            tilbakekrevingRepository.hentTilbakekreving(
                TilbakekrevingFilter.fagsakEllerTilbakekrevingId(fagsystemId, fagsystem),
            ),
        )
    }

    @Operation(summary = "Hent URL-er for alle tilbakekrevinger som mangler opprettet dato")
    @GetMapping(
        path = ["/statusmelding-med-manglende-opprettet-dato"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentUrlForSperredeSakerMedManglendeOpprettetDato(): Ressurs<List<String>> {
        if (!tilgangService.harInnloggetBrukerForvalterRolle()) {
            throw ForbiddenError(
                message = "Du har ikke forvaltertilgang",
                frontendFeilmelding = "Du har ikke forvaltertilgang",
                logContext = SecureLog.Context.tom(),
            )
        }
        return Ressurs.success(
            tilbakekrevingRepository.hentTilbakekrevingIderMedSperretStatusmelding()
                .mapNotNull { tilbakekrevingRepository.hentTilbakekreving(TilbakekrevingFilter.tilbakekreving(it)) }
                .map { it.fraEntity().hentTilbakekrevingUrl(applicationProperties.frontendUrl) },
        )
    }

    @Operation(summary = "Hent URL-er for alle tilbakekrevinger i ny modell for en fagsak")
    @GetMapping(
        path = ["/fagsystem/{fagsystem}/fagsak/{eksternFagsakId}/tilbakekreving-url"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentTilbakekrevingUrler(
        @PathVariable fagsystem: FagsystemDTO,
        @PathVariable eksternFagsakId: String,
    ): Ressurs<List<String>> {
        val behandler = ContextService.hentBehandler(SecureLog.Context.utenBehandling(eksternFagsakId))
        return Ressurs.success(
            tilbakekrevingRepository.hentTilbakekrevinger(TilbakekrevingFilter.fagsak(eksternFagsakId, fagsystem))
                .map { it.fraEntity() }
                .onEach { tilgangskontrollService.validerTilgangTilbakekreving(it, ValideringContext.ForvaltningHentTilbakekrevingUrler, behandler) }
                .map { it.hentTilbakekrevingUrl(applicationProperties.frontendUrl) },
        )
    }

    @Operation(summary = "Hent fagsystem-IDer i ny modell som har mottatt flere NY-kravgrunnlag")
    @GetMapping(
        path = ["/fagsystem-ider-med-flere-nye-kravgrunnlag"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentFagsystemIderMedFlereNyeKravgrunnlag(): Ressurs<List<TilbakekrevingRepository.FagsystemIdMedFlereNyeKravgrunnlag>> {
        if (!tilgangService.harInnloggetBrukerForvalterRolle()) {
            throw ForbiddenError(
                message = "Mangler forvaltertilgang til å hente fagsystem-IDer med flere nye kravgrunnlag",
                frontendFeilmelding = "Du har ikke tilgang til å hente fagsystem-IDer med flere nye kravgrunnlag",
                logContext = SecureLog.Context.tom(),
            )
        }
        return Ressurs.success(tilbakekrevingRepository.hentFagsystemIderMedFlereNyeKravgrunnlag())
    }

    @Operation(summary = "Hent fagsystem-IDer i ny modell der kravgrunnlag eller statusmeldinger har flere unike vedtak-IDer")
    @GetMapping(
        path = ["/fagsystem-ider-med-flere-vedtak-ider"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentFagsystemIderMedFlereVedtakIder(): Ressurs<List<TilbakekrevingRepository.FagsystemIdMedFlereVedtakIder>> {
        if (!tilgangService.harInnloggetBrukerForvalterRolle()) {
            throw ForbiddenError(
                message = "Mangler forvaltertilgang til å hente fagsystem-IDer med flere vedtak-IDer",
                frontendFeilmelding = "Du har ikke tilgang til å hente fagsystem-IDer med flere vedtak-IDer",
                logContext = SecureLog.Context.tom(),
            )
        }
        return Ressurs.success(tilbakekrevingRepository.hentFagsystemIderMedFlereVedtakIder())
    }

    @Operation(summary = "Hent kravgrunnlag i bufferet som ikke er koblet til en tilbakekreving i ny modell")
    @GetMapping(
        path = ["/finn-kravgrunnlag-uten-sak"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun finnKravgrunnlagUtenSak(): Ressurs<List<KravgrunnlagBufferRepository.KravgrunnlagUtenSak>> {
        if (!tilgangService.harInnloggetBrukerForvalterRolle()) {
            throw ForbiddenError(
                message = "Mangler forvaltertilgang til å hente kravgrunnlag uten sak",
                frontendFeilmelding = "Du har ikke tilgang til å hente kravgrunnlag uten sak",
                logContext = SecureLog.Context.tom(),
            )
        }
        return Ressurs.success(kravgrunnlagBufferRepository.finnKravgrunnlagUtenSak())
    }

    @Operation(summary = "Hent statusmeldinger i bufferet som ikke er koblet til en tilbakekreving i ny modell")
    @GetMapping(
        path = ["/finn-statusmeldinger-uten-sak"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun finnStatusmeldingerUtenSak(): Ressurs<List<StatusmeldingBufferRepository.StatusmeldingUtenSak>> {
        if (!tilgangService.harInnloggetBrukerForvalterRolle()) {
            throw ForbiddenError(
                message = "Mangler forvaltertilgang til å hente statusmeldinger uten sak",
                frontendFeilmelding = "Du har ikke tilgang til å hente statusmeldinger uten sak",
                logContext = SecureLog.Context.tom(),
            )
        }
        return Ressurs.success(statusmeldingBufferRepository.finnStatusmeldingerUtenSak())
    }

    @Operation(summary = "Hent URL for ytelser i gammel modell")
    @GetMapping(
        path = ["/behandling-url"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun lagBehandlingUrl(
        @RequestParam(required = false) vedtakId: BigInteger?,
        @RequestParam(required = false) vedtakGjelderId: String?,
        @RequestParam(required = false) fagsystemId: String?,
    ): Ressurs<List<String>> {
        val behandlingIder = kravgrunnlagRepository.findBehandlingIdsByVedtakIdOrGjelderVedtakIdOrFagsystemId(vedtakId, vedtakGjelderId, fagsystemId)
        behandlingIder.forEach {
            tilgangskontrollService.validerTilgangBehandlingID(
                behandlingId = it,
                minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                auditLoggerEvent = AuditLoggerEvent.NONE,
                handling = "Hent URL til behandling",
            )
        }
        val urler = behandlingIder.map { behandlingId ->
            val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
            val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
            "${applicationProperties.frontendUrl}/fagsystem/${fagsak.fagsystem.name}/fagsak/${fagsak.eksternFagsakId}/behandling/${behandling.eksternBrukId}/fakta"
        }
        return Ressurs.success(urler)
    }

    @PostMapping("/bytt-institusjon/{behandlingId}")
    fun oppdaterInstutisjon(
        @PathVariable behandlingId: UUID,
        @RequestBody orgnummer: String,
    ) {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.FORVALTER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Manuell oppdatering av instutisjon",
        )
        fagsakRepository
        fagsakRepository.update(
            fagsakRepository.finnFagsakForBehandlingId(behandlingId)
                .copy(institusjon = Institusjon(orgnummer)),
        )
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

data class BufferMeldinger(
    val kravgrunnlag: List<BufferKravgrunnlag>,
    val statusmeldinger: List<BufferStatusmelding>,
)

data class BufferKravgrunnlag(
    val kravgrunnlagId: String,
    val lest: Boolean,
    val mottatt: LocalDateTime,
    val melding: String,
)

data class BufferStatusmelding(
    val id: UUID,
    val vedtakId: String,
    val status: String,
    val lest: Boolean,
    val mottatt: LocalDateTime,
    val melding: String,
)

data class MottatteMeldinger(
    val mottatte: List<MottattMelding>,
    val arkiverte: List<ArkivertMelding>,
)

data class MottattMelding(
    val mottattXmlId: UUID,
    val ytelsestype: Ytelsestype,
    val eksternKravgrunnlagId: BigInteger?,
    val vedtakId: BigInteger,
    val kravstatuskode: String,
    val referanse: String,
    val sperret: Boolean,
    val opprettetTid: LocalDateTime,
    val melding: String,
)

data class ArkivertMelding(
    val arkivId: UUID,
    val ytelsestype: Ytelsestype,
    val gammelMottattXmlId: UUID?,
    val opprettetTid: LocalDateTime,
    val melding: String,
)

data class Kravgrunnlagsinfo(
    val eksternKravgrunnlagId: BigInteger,
    val kravgrunnlagKravstatuskode: String,
    val mottattXmlId: UUID?,
    val eksternId: String,
    val opprettetTid: LocalDateTime,
)
