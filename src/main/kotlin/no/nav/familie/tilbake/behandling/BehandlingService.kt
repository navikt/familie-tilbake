package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Brevmottaker
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandling
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.api.dto.ByttEnhetDto
import no.nav.familie.tilbake.api.dto.HenleggelsesbrevFritekstDto
import no.nav.familie.tilbake.api.dto.OpprettRevurderingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype.REVURDERING_TILBAKEKREVING
import no.nav.familie.tilbake.behandling.domain.Behandlingstype.TILBAKEKREVING
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Fagsystemskonsekvens
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandling.task.OpprettBehandlingManueltTask
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.SendHenleggelsesbrevTask
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerMapper
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.varsel.SendVarselbrevTask
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.task.FinnKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.HentKravgrunnlagTask
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val brevsporingService: BrevsporingService,
    private val manuellBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val behandlingTilstandService: BehandlingTilstandService,
    private val tellerService: TellerService,
    private val stegService: StegService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val historikkTaskService: HistorikkTaskService,
    private val tilgangService: TilgangService,
    @Value("\${OPPRETTELSE_DAGER_BEGRENSNING:6}")
    private val opprettelseDagerBegrensning: Long,
    private val integrasjonerClient: IntegrasjonerClient,
    private val validerBehandlingService: ValiderBehandlingService,
    private val oppgaveService: OppgaveService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun opprettBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        val behandling: Behandling = opprettFørstegangsbehandling(opprettTilbakekrevingRequest)

        if (opprettTilbakekrevingRequest.faktainfo.tilbakekrevingsvalg ===
            Tilbakekrevingsvalg
                .OPPRETT_TILBAKEKREVING_MED_VARSEL &&
            !behandling.manueltOpprettet
        ) {
            val sendVarselbrev =
                Task(
                    type = SendVarselbrevTask.TYPE,
                    payload = behandling.id.toString(),
                    properties =
                        Properties().apply {
                            setProperty(PropertyName.FAGSYSTEM, opprettTilbakekrevingRequest.fagsystem.name)
                        },
                )
            taskService.save(sendVarselbrev)
        }

        return behandling
    }

    @Transactional
    fun opprettBehandlingManuellTask(opprettManueltTilbakekrevingRequest: OpprettManueltTilbakekrevingRequest) {
        val kanBehandlingOpprettesManuelt =
            fagsakService.kanBehandlingOpprettesManuelt(
                opprettManueltTilbakekrevingRequest.eksternFagsakId,
                opprettManueltTilbakekrevingRequest.ytelsestype,
            )
        if (!kanBehandlingOpprettesManuelt.kanBehandlingOpprettes) {
            throw Feil(message = kanBehandlingOpprettesManuelt.melding)
        }
        logger.info("Oppretter OpprettBehandlingManueltTask for request=$opprettManueltTilbakekrevingRequest")
        val properties =
            Properties().apply {
                setProperty("eksternFagsakId", opprettManueltTilbakekrevingRequest.eksternFagsakId)
                setProperty("ytelsestype", opprettManueltTilbakekrevingRequest.ytelsestype.name)
                setProperty("eksternId", opprettManueltTilbakekrevingRequest.eksternId)
                setProperty(
                    PropertyName.FAGSYSTEM,
                    FagsystemUtil.hentFagsystemFraYtelsestype(opprettManueltTilbakekrevingRequest.ytelsestype).name,
                )
                setProperty("ansvarligSaksbehandler", ContextService.hentSaksbehandler())
            }
        taskService.save(
            Task(
                type = OpprettBehandlingManueltTask.TYPE,
                properties = properties,
                payload = "",
            ),
        )
    }

    @Transactional
    fun opprettRevurdering(opprettRevurderingDto: OpprettRevurderingDto): Behandling {
        val originalBehandlingId = opprettRevurderingDto.originalBehandlingId
        logger.info("Oppretter revurdering for behandling $originalBehandlingId")
        val originalBehandling = behandlingRepository.findByIdOrThrow(originalBehandlingId)
        if (!kanRevurderingOpprettes(originalBehandling)) {
            val feilmelding =
                "Revurdering kan ikke opprettes for behandling $originalBehandlingId. " +
                    "Enten behandlingen er ikke avsluttet med kravgrunnlag eller " +
                    "det finnes allerede en åpen revurdering"
            throw Feil(message = feilmelding, frontendFeilmelding = feilmelding)
        }
        val revurdering =
            BehandlingMapper.tilDomeneBehandlingRevurdering(originalBehandling, opprettRevurderingDto.årsakstype)
        behandlingRepository.insert(revurdering)

        val fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(opprettRevurderingDto.ytelsestype)
        historikkTaskService.lagHistorikkTask(
            revurdering.id,
            BEHANDLING_OPPRETTET,
            Aktør.SAKSBEHANDLER,
        )

        behandlingskontrollService.fortsettBehandling(revurdering.id)
        stegService.håndterSteg(revurdering.id)

        // kjør HentKravgrunnlagTask for å hente kravgrunnlag på nytt fra økonomi
        taskService.save(
            Task(
                type = HentKravgrunnlagTask.TYPE,
                payload = revurdering.id.toString(),
                properties = Properties().apply { setProperty(PropertyName.FAGSYSTEM, fagsystem.name) },
            ),
        )

        // Lag oppgave for behandling
        oppgaveTaskService.opprettOppgaveTask(revurdering, Oppgavetype.BehandleSak)

        return revurdering
    }

    @Transactional(readOnly = true)
    fun hentBehandling(behandlingId: UUID): BehandlingDto {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val erBehandlingPåVent: Boolean = behandlingskontrollService.erBehandlingPåVent(behandling.id)
        val behandlingsstegsinfoer: List<Behandlingsstegsinfo> =
            behandlingskontrollService
                .hentBehandlingsstegstilstand(behandling)
        val varselSendt = brevsporingService.erVarselSendt(behandlingId)
        val kanBehandlingHenlegges: Boolean = kanHenleggeBehandling(behandling)
        val kanEndres: Boolean = kanBehandlingEndres(behandling, fagsak.fagsystem)
        val kanSetteBehandlingTilbakeTilFakta = kanSetteBehandlingTilbakeTilFakta(behandling, finnBehandlerrolle(fagsak))
        val kanRevurderingOpprettes: Boolean =
            tilgangService.tilgangTilÅOppretteRevurdering(fagsak.fagsystem) && kanRevurderingOpprettes(behandling)
        val støtterManuelleBrevmottakere =
            sjekkOmManuelleBrevmottakereErStøttet(
                behandling = behandling,
                fagsak = fagsak,
            )
        val manuelleBrevmottakere =
            if (støtterManuelleBrevmottakere) {
                manuellBrevmottakerRepository.findByBehandlingId(behandlingId)
            } else {
                emptyList()
            }

        return BehandlingMapper.tilRespons(
            behandling,
            erBehandlingPåVent,
            kanBehandlingHenlegges,
            kanEndres,
            kanSetteBehandlingTilbakeTilFakta,
            kanRevurderingOpprettes,
            behandlingsstegsinfoer,
            varselSendt,
            fagsak.eksternFagsakId,
            manuelleBrevmottakere,
            støtterManuelleBrevmottakere,
        )
    }

    private fun finnBehandlerrolle(fagsak: Fagsak) = tilgangService.finnBehandlerrolle(fagsak.fagsystem) ?: throw Feil("Kunne ikke finne Behandlerrolle")

    @Transactional
    fun settBehandlingPåVent(
        behandlingId: UUID,
        behandlingPåVentDto: BehandlingPåVentDto,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)

        if (LocalDate.now() >= behandlingPåVentDto.tidsfrist) {
            throw Feil(
                message = "Fristen må være større enn dagens dato for behandling $behandlingId",
                frontendFeilmelding = "Fristen må være større enn dagens dato for behandling $behandlingId",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        oppdaterAnsvarligSaksbehandler(behandlingId)

        behandlingskontrollService.settBehandlingPåVent(
            behandlingId,
            behandlingPåVentDto.venteårsak,
            behandlingPåVentDto.tidsfrist,
        )

        val beskrivelse =
            when (behandlingPåVentDto.venteårsak) {
                Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING -> "Frist er oppdatert pga mottatt tilbakemelding fra bruker"
                Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG -> "Ny frist satt på bakgrunn av mottatt kravgrunnlag fra økonomi"
                else -> "Frist er oppdatert av saksbehandler ${ContextService.hentSaksbehandler()}"
            }
        oppgaveTaskService.oppdaterOppgaveTask(
            behandlingId,
            beskrivelse,
            behandlingPåVentDto.tidsfrist,
            ContextService.hentSaksbehandler(),
        )
    }

    @Transactional
    fun taBehandlingAvvent(behandlingId: UUID) { // Denne metoden brukes kun for å gjenoppta behandling manuelt av saksbehandler
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)

        if (!behandlingskontrollService.erBehandlingPåVent(behandlingId)) {
            throw Feil(
                message = "Behandling $behandlingId er ikke på vent, kan ike gjenoppta",
                frontendFeilmelding = "Behandling $behandlingId er ikke på vent, kan ike gjenoppta",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        oppdaterAnsvarligSaksbehandler(behandlingId)

        historikkTaskService.lagHistorikkTask(
            behandling.id,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
            Aktør.SAKSBEHANDLER,
        )

        stegService.gjenopptaSteg(behandlingId)
        oppgaveTaskService.oppdaterOppgaveTask(
            behandlingId = behandlingId,
            beskrivelse = "Behandling er tatt av vent",
            frist = LocalDate.now(),
            saksbehandler = ContextService.hentSaksbehandler(),
        )

        // oppdaterer oppgave hvis saken er fortsatt på vent,
        // f.eks saken var på vent med brukerstilbakemelding og har ikke fått kravgrunnlag
        val aktivStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        if (aktivStegstilstand?.behandlingsstegsstatus == Behandlingsstegstatus.VENTER) {
            oppgaveTaskService.oppdaterOppgaveTaskMedTriggertid(
                behandlingId = behandlingId,
                beskrivelse = aktivStegstilstand.venteårsak!!.beskrivelse,
                frist = aktivStegstilstand.tidsfrist!!,
                triggerTid = 2L,
            )
        }
    }

    @Transactional
    fun henleggBehandling(
        behandlingId: UUID,
        henleggelsesbrevFritekstDto: HenleggelsesbrevFritekstDto,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)

        val behandlingsresultatstype = henleggelsesbrevFritekstDto.behandlingsresultatstype

        if (!kanHenleggeBehandling(behandling, behandlingsresultatstype)) {
            throw Feil(
                message = "Behandling med behandlingId=$behandlingId kan ikke henlegges.",
                frontendFeilmelding = "Behandling med behandlingId=$behandlingId kan ikke henlegges.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        // oppdaterer behandlingsstegstilstand
        behandlingskontrollService.henleggBehandlingssteg(behandlingId)

        // oppdaterer behandlingsresultat og behandling
        behandlingRepository.update(
            behandling.copy(
                resultater = setOf(Behandlingsresultat(type = behandlingsresultatstype)),
                status = Behandlingsstatus.AVSLUTTET,
                avsluttetDato = LocalDate.now(),
            ),
        )

        oppdaterAnsvarligSaksbehandler(behandlingId)
        behandlingTilstandService.opprettSendingAvBehandlingenHenlagt(behandlingId)
        val fagsystem = fagsakService.finnFagsystem(behandling.fagsakId)

        val aktør =
            when (behandlingsresultatstype) {
                Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
                Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                -> Aktør.VEDTAKSLØSNING

                else -> Aktør.SAKSBEHANDLER
            }
        historikkTaskService.lagHistorikkTask(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
            aktør = aktør,
            beskrivelse = henleggelsesbrevFritekstDto.begrunnelse,
        )

        if (kanSendeHenleggelsesbrev(behandling, behandlingsresultatstype)) {
            taskService.save(
                SendHenleggelsesbrevTask.opprettTask(
                    behandlingId,
                    fagsystem,
                    henleggelsesbrevFritekstDto.fritekst,
                ),
            )
        }

        // Ferdigstill oppgave
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId)
        tellerService.tellVedtak(Behandlingsresultatstype.HENLAGT, behandling)
    }

    @Transactional
    fun oppdaterAnsvarligSaksbehandler(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val gjeldendeSaksbehandler = ContextService.hentSaksbehandler()
        if (behandling.ansvarligSaksbehandler != gjeldendeSaksbehandler) {
            behandlingRepository.update(behandling.copy(ansvarligSaksbehandler = gjeldendeSaksbehandler))
        }
    }

    @Transactional
    fun oppdaterFaktainfo(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
        respons: HentFagsystemsbehandling,
    ) {
        val behandling =
            behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId)
                ?: throw Feil(
                    "Det finnes ikke en åpen behandling for " +
                        "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype",
                )
        val faktainfo = respons.faktainfo
        val fagsystemskonsekvenser =
            faktainfo.konsekvensForYtelser.map { Fagsystemskonsekvens(konsekvens = it) }.toSet()
        if (behandling.aktivFagsystemsbehandling.eksternId == eksternId) {
            logger.info(
                "Det trenger ikke å oppdatere fakta info siden tilbakekrevingsbehandling " +
                    "er allerede koblet med riktig fagsystemsbehandling",
            )
            return
        }
        val gammelFagsystemsbehandling = behandling.aktivFagsystemsbehandling.copy(aktiv = false)
        val nyFagsystemsbehandling =
            Fagsystemsbehandling(
                eksternId = eksternId,
                årsak = faktainfo.revurderingsårsak,
                resultat = faktainfo.revurderingsresultat,
                // kopier gammel tilbakekrevingsvalg om det ikke finnes i fagsystem
                tilbakekrevingsvalg =
                    faktainfo.tilbakekrevingsvalg
                        ?: gammelFagsystemsbehandling.tilbakekrevingsvalg,
                revurderingsvedtaksdato = respons.revurderingsvedtaksdato,
                konsekvenser = fagsystemskonsekvenser,
            )
        behandlingRepository.update(
            behandling.copy(
                fagsystemsbehandling =
                    setOf(
                        gammelFagsystemsbehandling,
                        nyFagsystemsbehandling,
                    ),
                regelverk = respons.regelverk,
            ),
        )
    }

    @Transactional
    fun oppdaterSaksbehandlingtype(
        behandlingId: UUID,
        saksbehandlingstype: Saksbehandlingstype,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(saksbehandlingstype = Saksbehandlingstype.ORDINÆR))
    }

    @Transactional
    fun byttBehandlendeEnhet(
        behandlingId: UUID,
        byttEnhetDto: ByttEnhetDto,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)
        val fagsystem = fagsakService.finnFagsystem(behandling.fagsakId)
        if (fagsystem != Fagsystem.BA) {
            throw Feil(
                message = "Ikke implementert for fagsystem $fagsystem",
                frontendFeilmelding = "Ikke implementert for fagsystem: ${fagsystem.navn}",
            )
        }
        val enhet = integrasjonerClient.hentNavkontor(byttEnhetDto.enhet)
        behandlingRepository.update(
            behandling.copy(
                behandlendeEnhet = byttEnhetDto.enhet,
                behandlendeEnhetsNavn = enhet.navn,
            ),
        )
        oppdaterAnsvarligSaksbehandler(behandlingId)

        historikkTaskService.lagHistorikkTask(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET,
            aktør = Aktør.SAKSBEHANDLER,
            beskrivelse = byttEnhetDto.begrunnelse,
        )

        oppgaveTaskService.oppdaterEnhetOppgaveTask(
            behandlingId = behandlingId,
            beskrivelse = "Endret tildelt enhet: " + byttEnhetDto.enhet,
            enhetId = byttEnhetDto.enhet,
        )
    }

    private fun opprettFørstegangsbehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        validerBehandlingService.validerOpprettBehandling(opprettTilbakekrevingRequest)

        val fagsystem = opprettTilbakekrevingRequest.fagsystem
        val tilbakekrevingsvalgErAutomatisk = opprettTilbakekrevingRequest.faktainfo.tilbakekrevingsvalg == Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_AUTOMATISK

        logOppretterBehandling(tilbakekrevingsvalgErAutomatisk, opprettTilbakekrevingRequest)

        val fagsak = finnEllerOpprettFagsak(opprettTilbakekrevingRequest)
        val behandling = lagreBehandling(opprettTilbakekrevingRequest, fagsak, tilbakekrevingsvalgErAutomatisk)
        historikkTaskService.lagHistorikkTask(behandling.id, BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        behandlingskontrollService.fortsettBehandling(behandling.id)
        stegService.håndterSteg(behandling.id)

        opprettBehandleBrevmottakerSteg(opprettTilbakekrevingRequest.manuelleBrevmottakere, behandling, fagsystem, fagsak)

        // kjør FinnGrunnlagTask for å finne og koble grunnlag med behandling
        taskService.save(
            Task(
                type = FinnKravgrunnlagTask.TYPE,
                payload = behandling.id.toString(),
                properties = Properties().apply { setProperty(PropertyName.FAGSYSTEM, fagsystem.name) },
            ),
        )

        if (!tilbakekrevingsvalgErAutomatisk) {
            oppgaveTaskService.opprettOppgaveTask(behandling, Oppgavetype.BehandleSak)
        }

        return behandling
    }

    private fun logOppretterBehandling(
        erAutomatisk: Boolean,
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
    ) {
        val erAutomatiskLogg = if (erAutomatisk) "som skal behandles automatisk" else ""
        logger.info(
            "Oppretter Tilbakekrevingsbehandling $erAutomatiskLogg for ytelsestype=${opprettTilbakekrevingRequest.ytelsestype},eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId} " +
                "og eksternId=${opprettTilbakekrevingRequest.eksternId}",
        )
        secureLogger.info(
            "Oppretter Tilbakekrevingsbehandling $erAutomatiskLogg for ytelsestype=${opprettTilbakekrevingRequest.ytelsestype},eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId} " +
                " og personIdent=${opprettTilbakekrevingRequest.personIdent}",
        )
    }

    private fun finnEllerOpprettFagsak(
        request: OpprettTilbakekrevingRequest,
    ): Fagsak {
        val eksisterendeFagsak = fagsakService.finnFagsak(request.fagsystem, request.eksternFagsakId)
        val fagsak =
            eksisterendeFagsak
                ?: fagsakService.opprettFagsak(request, request.ytelsestype, request.fagsystem)
        return fagsak
    }

    private fun lagreBehandling(
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
        fagsak: Fagsak,
        erAutomatiskOgFeatureTogglePå: Boolean,
    ): Behandling {
        val ansvarligsaksbehandler =
            integrasjonerClient.hentSaksbehandler(opprettTilbakekrevingRequest.saksbehandlerIdent)
        val behandling =
            BehandlingMapper.tilDomeneBehandling(
                opprettTilbakekrevingRequest,
                opprettTilbakekrevingRequest.fagsystem,
                fagsak,
                ansvarligsaksbehandler,
                erAutomatiskOgFeatureTogglePå,
            )
        behandlingRepository.insert(behandling)
        return behandling
    }

    private fun opprettBehandleBrevmottakerSteg(
        brevmottakere: Set<Brevmottaker>,
        behandling: Behandling,
        fagsystem: Fagsystem,
        fagsak: Fagsak,
    ) {
        val manuelleBrevmottakere = brevmottakere.map { brevmottaker -> ManuellBrevmottakerMapper.tilDomene(brevmottaker, behandling.id) }
        if (manuelleBrevmottakere.isNotEmpty()) {
            logger.info("Lagrer ${manuelleBrevmottakere.size} manuell(e) brevmottaker(e) oversendt fra $fagsystem-sak")
            manuellBrevmottakerRepository.insertAll(manuelleBrevmottakere)
            if (sjekkOmManuelleBrevmottakereErStøttet(behandling, fagsak)) {
                behandlingskontrollService.behandleBrevmottakerSteg(behandling.id)
            }
        }
    }

    private fun kanHenleggeBehandling(
        behandling: Behandling,
        behandlingsresultatstype: Behandlingsresultatstype? = null,
    ): Boolean {
        if (Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT == behandlingsresultatstype ||
            Behandlingsresultatstype.HENLAGT_MANGLENDE_KRAVGRUNNLAG == behandlingsresultatstype
        ) {
            return true
        } else if (TILBAKEKREVING == behandling.type) {
            return !behandling.erAvsluttet &&
                (
                    !behandling.manueltOpprettet &&
                        behandling.opprettetTidspunkt <
                        LocalDate
                            .now()
                            .atStartOfDay()
                            .minusDays(opprettelseDagerBegrensning)
                ) &&
                !kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id)
        }
        return true
    }

    private fun kanSendeHenleggelsesbrev(
        behandling: Behandling,
        behandlingsresultatstype: Behandlingsresultatstype,
    ): Boolean =
        when (behandling.type) {
            TILBAKEKREVING -> brevsporingService.erVarselSendt(behandling.id)
            REVURDERING_TILBAKEKREVING -> Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_MED_BREV == behandlingsresultatstype
        }

    private fun sjekkOmBehandlingAlleredeErAvsluttet(behandling: Behandling) {
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil(
                "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun kanBehandlingEndres(
        behandling: Behandling,
        fagsystem: Fagsystem,
    ): Boolean {
        if (behandling.erSaksbehandlingAvsluttet) {
            return false
        }
        if (Behandlingsstatus.FATTER_VEDTAK == behandling.status &&
            behandling.ansvarligSaksbehandler == ContextService.hentSaksbehandler()
        ) {
            return false
        }

        return when (tilgangService.finnBehandlerrolle(fagsystem)) {
            Behandlerrolle.SAKSBEHANDLER -> (Behandlingsstatus.UTREDES == behandling.status)
            Behandlerrolle.BESLUTTER, Behandlerrolle.SYSTEM -> true
            else -> false
        }
    }

    private fun kanSetteBehandlingTilbakeTilFakta(
        behandling: Behandling,
        behandlerRolle: Behandlerrolle,
    ): Boolean =
        behandlingUtredesOgErIkkePåVent(behandling) &&
            harInnloggetBrukerTilgangTilÅSetteTilbakeTilFakta(behandling.ansvarligSaksbehandler, behandlerRolle)

    private fun harInnloggetBrukerTilgangTilÅSetteTilbakeTilFakta(
        ansvarligSaksbehandler: String,
        behandlerRolle: Behandlerrolle,
    ) = erAnsvarligSaksbehandler(ansvarligSaksbehandler, behandlerRolle) || tilgangService.harInnloggetBrukerForvalterRolle()

    private fun erAnsvarligSaksbehandler(
        ansvarligSaksbehandler: String,
        behandlerRolle: Behandlerrolle,
    ) = ContextService.hentSaksbehandler() == ansvarligSaksbehandler &&
        (behandlerRolle == Behandlerrolle.SAKSBEHANDLER || behandlerRolle == Behandlerrolle.BESLUTTER)

    private fun behandlingUtredesOgErIkkePåVent(behandling: Behandling) = !behandlingskontrollService.erBehandlingPåVent(behandling.id)

    private fun kanRevurderingOpprettes(behandling: Behandling): Boolean =
        behandling.erAvsluttet &&
            !Behandlingsresultat.ALLE_HENLEGGELSESKODER.contains(behandling.sisteResultat?.type) &&
            kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id) &&
            behandlingRepository.finnÅpenTilbakekrevingsrevurdering(behandling.id) == null

    @Transactional
    fun angreSendTilBeslutter(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerKanAngreSendTilBeslutter(behandling)

        historikkTaskService.lagHistorikkTask(
            behandling.id,
            TilbakekrevingHistorikkinnslagstype.ANGRE_SEND_TIL_BESLUTTER,
            Aktør.SAKSBEHANDLER,
        )

        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId, Oppgavetype.GodkjenneVedtak.name)
        oppgaveTaskService.opprettOppgaveTask(behandling, Oppgavetype.BehandleSak)

        stegService.angreSendTilBeslutter(behandling)
    }

    private fun validerKanAngreSendTilBeslutter(behandling: Behandling) {
        val innloggetSaksbehandler = ContextService.hentSaksbehandler()
        val saksbehandlerSendtTilBeslutter = behandling.ansvarligSaksbehandler

        if (saksbehandlerSendtTilBeslutter != innloggetSaksbehandler) {
            throw Feil(
                "Prøver å angre på at behandling id=${behandling.id} er sendt til beslutter, men er ikke ansvarlig saksbehandler på behandlingen.",
                frontendFeilmelding = "Kan kun angre send til beslutter dersom du er saksbehandler på vedtaket.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        val godkjenneVedtakOppgave =
            oppgaveService.hentOppgaveSomIkkeErFerdigstilt(
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                behandling = behandling,
            ) ?: throw Feil("Systemet har ikke rukket å opprette godkjenne vedtak oppgaven ennå, kan ikke angre send til beslutter", frontendFeilmelding = "Systemet har ikke rukket å opprette godkjenne vedtak oppgaven enda. Prøv igjen om litt.", httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)

        val tilordnetRessurs = godkjenneVedtakOppgave.tilordnetRessurs
        val oppgaveErTilordnetEnAnnenSaksbehandler =
            tilordnetRessurs != null && tilordnetRessurs != innloggetSaksbehandler
        if (oppgaveErTilordnetEnAnnenSaksbehandler) {
            throw Feil("Kan ikke angre send til beslutter, oppgaven er plukket av $tilordnetRessurs", frontendFeilmelding = "Kan ikke angre send til beslutter, oppgaven er plukket av $tilordnetRessurs", httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    companion object {
        fun sjekkOmManuelleBrevmottakereErStøttet(
            behandling: Behandling,
            fagsak: Fagsak,
        ): Boolean = fagsak.institusjon == null && !behandling.harVerge
    }
}
