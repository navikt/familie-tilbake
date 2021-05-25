package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype.REVURDERING_TILBAKEKREVING
import no.nav.familie.tilbake.behandling.domain.Behandlingstype.TILBAKEKREVING
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.henleggelse.SendHenleggelsesbrevTask
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.SendVarselbrevTask
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val fagsakRepository: FagsakRepository,
                        private val taskRepository: TaskRepository,
                        private val brevsporingRepository: BrevsporingRepository,
                        private val kravgrunnlagRepository: KravgrunnlagRepository,
                        private val behandlingskontrollService: BehandlingskontrollService,
                        private val stegService: StegService,
                        private val oppgaveTaskService: OppgaveTaskService,
                        private val historikkTaskService: HistorikkTaskService,
                        private val rolleConfig: RolleConfig) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun opprettBehandlingAutomatisk(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        val behandling: Behandling = opprettFørstegangsbehandling(opprettTilbakekrevingRequest)

        //Lag oppgave for behandling
        oppgaveTaskService.opprettOppgaveTask(behandling.id, Oppgavetype.BehandleSak)

        // historikkinnslag
        historikkTaskService.lagHistorikkTask(behandling.id,
                                              TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET,
                                              Aktør.VEDTAKSLØSNING)

        if (opprettTilbakekrevingRequest.faktainfo.tilbakekrevingsvalg === Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)  {
            val sendVarselbrev = Task(type = SendVarselbrevTask.TYPE,
                                      payload = behandling.id.toString())
            taskRepository.save(sendVarselbrev)
        }

        return behandling
    }

    fun opprettBehandlingManuell(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        TODO("Ikke implementert ennå")
    }

    @Transactional(readOnly = true)
    fun hentBehandling(behandlingId: UUID): BehandlingDto {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val erBehandlingPåVent: Boolean = behandlingskontrollService.erBehandlingPåVent(behandling.id)
        val behandlingsstegsinfoer: List<Behandlingsstegsinfo> = behandlingskontrollService
                .hentBehandlingsstegstilstand(behandling)
        val varselSendt = brevsporingRepository.existsByBehandlingIdAndBrevtypeIn(behandlingId, setOf(Brevtype.VARSEL,
                                                                                                      Brevtype.KORRIGERT_VARSEL))
        val kanBehandlingHenlegges: Boolean = kanHenleggeBehandling(behandling)
        val kanEndres: Boolean = kanBehandlingEndres(behandling, fagsak.fagsystem)

        return BehandlingMapper.tilRespons(behandling,
                                           erBehandlingPåVent,
                                           kanBehandlingHenlegges,
                                           kanEndres,
                                           behandlingsstegsinfoer,
                                           varselSendt)
    }

    @Transactional
    fun settBehandlingPåVent(behandlingId: UUID, behandlingPåVentDto: BehandlingPåVentDto) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)

        if (LocalDate.now() >= behandlingPåVentDto.tidsfrist) {
            throw Feil(message = "Fristen må være større enn dagens dato for behandling $behandlingId",
                       frontendFeilmelding = "Fristen må være større enn dagens dato for behandling $behandlingId",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
        behandlingskontrollService.settBehandlingPåVent(behandlingId,
                                                        behandlingPåVentDto.venteårsak,
                                                        behandlingPåVentDto.tidsfrist)
        oppdaterAnsvarligSaksbehandler(behandlingId)

        // historikkinnslag
        historikkTaskService.lagHistorikkTask(behandling.id,
                                              TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
                                              Aktør.SAKSBEHANDLER)
    }

    @Transactional
    fun taBehandlingAvvent(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)

        if (!behandlingskontrollService.erBehandlingPåVent(behandlingId)) {
            throw Feil(message = "Behandling $behandlingId er ikke på vent, kan ike gjenoppta",
                       frontendFeilmelding = "Behandling $behandlingId er ikke på vent, kan ike gjenoppta",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
        stegService.gjenopptaSteg(behandlingId)

        oppdaterAnsvarligSaksbehandler(behandlingId)

        // historikkinnslag
        historikkTaskService.lagHistorikkTask(behandling.id,
                                              TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
                                              Aktør.SAKSBEHANDLER)
    }

    @Transactional
    fun henleggBehandling(behandlingId: UUID,
                          behandlingsresultatstype: Behandlingsresultatstype,
                          fritekst: String? = null) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)

        if (!kanHenleggeBehandling(behandling, behandlingsresultatstype)) {
            throw Feil(message = "Behandling med behandlingId=$behandlingId kan ikke henlegges.",
                       frontendFeilmelding = "Behandling med behandlingId=$behandlingId kan ikke henlegges.",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

        //oppdaterer behandlingsstegstilstand
        behandlingskontrollService.henleggBehandlingssteg(behandlingId)

        //oppdaterer behandlingsresultat og behandling
        behandlingRepository.update(behandling.copy(resultater = setOf(Behandlingsresultat(type = behandlingsresultatstype)),
                                                    status = Behandlingsstatus.AVSLUTTET,
                                                    avsluttetDato = LocalDate.now()))

        //historikkinnslag
        val aktør = when (behandlingsresultatstype) {
            Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
            Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD -> Aktør.VEDTAKSLØSNING
            else -> Aktør.SAKSBEHANDLER
        }
        historikkTaskService.lagHistorikkTask(behandlingId,
                                              TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
                                              aktør)

        if (kanSendeHenleggelsesbrev(behandling, behandlingsresultatstype)) {
            taskRepository.save(SendHenleggelsesbrevTask.opprettTask(behandlingId, fritekst))
        }

        // Ferdigstill oppgave
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId, Oppgavetype.BehandleSak)
    }

    @Transactional
    fun oppdaterAnsvarligSaksbehandler(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(ansvarligSaksbehandler = ContextService.hentSaksbehandler()))
    }

    private fun opprettFørstegangsbehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        val ytelsestype = opprettTilbakekrevingRequest.ytelsestype
        val fagsystem = opprettTilbakekrevingRequest.fagsystem
        validateFagsystem(ytelsestype, fagsystem)
        val eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId
        val eksternId = opprettTilbakekrevingRequest.eksternId
        logger.info("Oppretter Tilbakekrevingsbehandling for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
                    "og eksternId=$eksternId")
        secureLogger.info("Oppretter Tilbakekrevingsbehandling for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
                          " og personIdent=${opprettTilbakekrevingRequest.personIdent}")

        kanBehandlingOpprettes(ytelsestype, eksternFagsakId, eksternId)
        // oppretter fagsak
        val fagsak = opprettFagsak(opprettTilbakekrevingRequest, ytelsestype, fagsystem)
        fagsakRepository.insert(fagsak)
        val behandling = BehandlingMapper.tilDomeneBehandling(opprettTilbakekrevingRequest, fagsystem, fagsak)
        behandlingRepository.insert(behandling)

        behandlingskontrollService.fortsettBehandling(behandling.id)
        stegService.håndterSteg(behandling.id)

        return behandling
    }

    private fun validateFagsystem(ytelsestype: Ytelsestype,
                                  fagsystem: Fagsystem) {
        if (FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype) != fagsystem) {
            throw Feil(message = "Behandling kan ikke opprettes med ytelsestype=$ytelsestype og fagsystem=$fagsystem",
                       frontendFeilmelding = "Behandling kan ikke opprettes med ytelsestype=$ytelsestype og fagsystem=$fagsystem",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    private fun kanBehandlingOpprettes(ytelsestype: Ytelsestype,
                                       eksternFagsakId: String,
                                       eksternId: String) {
        val behandling: Behandling? = behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId)
        if (behandling != null) {
            val feilMelding = "Det finnes allerede en åpen behandling for ytelsestype=$ytelsestype " +
                              "og eksternFagsakId=$eksternFagsakId, kan ikke opprette en ny."
            throw Feil(message = feilMelding, frontendFeilmelding = feilMelding,
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

        //hvis behandlingen er henlagt, kan det opprettes ny behandling
        val avsluttetBehandlinger = behandlingRepository.finnAvsluttetTilbakekrevingsbehandlinger(eksternId)
        if (avsluttetBehandlinger.isNotEmpty()) {
            val sisteAvsluttetBehandling: Behandling = avsluttetBehandlinger.first()
            val erSisteBehandlingHenlagt: Boolean =
                    sisteAvsluttetBehandling.resultater.any { Behandlingsresultat().erBehandlingHenlagt() }
            if (!erSisteBehandlingHenlagt) {
                val feilMelding = "Det finnes allerede en avsluttet behandling for ytelsestype=$ytelsestype " +
                                  "og eksternFagsakId=$eksternFagsakId som ikke er henlagt, kan ikke opprette en ny."
                throw Feil(message = feilMelding, frontendFeilmelding = feilMelding,
                           httpStatus = HttpStatus.BAD_REQUEST)
            }
        }
    }

    private fun opprettFagsak(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
                              ytelsestype: Ytelsestype,
                              fagsystem: Fagsystem): Fagsak {
        val bruker = Bruker(ident = opprettTilbakekrevingRequest.personIdent,
                            språkkode = opprettTilbakekrevingRequest.språkkode)
        return Fagsak(bruker = bruker,
                      eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId,
                      ytelsestype = ytelsestype,
                      fagsystem = fagsystem)
    }

    private fun kanHenleggeBehandling(behandling: Behandling,
                                      behandlingsresultatstype: Behandlingsresultatstype? = null): Boolean {
        if (Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT == behandlingsresultatstype) {
            return true
        } else if (TILBAKEKREVING == behandling.type) {
            return !behandling.erAvsluttet && (!behandling.manueltOpprettet &&
                                                 behandling.opprettetTidspunkt < LocalDate.now()
                                                         .atStartOfDay()
                                                         .minusDays(OPPRETTELSE_DAGER_BEGRENSNING))
                   && !kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id)
        }
        return true
    }

    private fun kanSendeHenleggelsesbrev(behandling: Behandling, behandlingsresultatstype: Behandlingsresultatstype): Boolean {
        when (behandling.type) {
            TILBAKEKREVING -> {
                if (brevsporingRepository.existsByBehandlingIdAndBrevtypeIn(behandling.id,
                                                                            setOf(Brevtype.VARSEL, Brevtype.KORRIGERT_VARSEL))) {
                    return true
                }
            }
            REVURDERING_TILBAKEKREVING -> {
                if (Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_MED_BREV == behandlingsresultatstype) {
                    return true
                }
            }
        }
        return false
    }

    private fun sjekkOmBehandlingAlleredeErAvsluttet(behandling: Behandling) {
        if (behandling.erAvsluttet) {
            throw Feil("Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    companion object {

        const val OPPRETTELSE_DAGER_BEGRENSNING = 6L
    }

    private fun kanBehandlingEndres(behandling: Behandling, fagsystem: Fagsystem): Boolean {
        if (behandling.erAvsluttet || behandling.status == Behandlingsstatus.IVERKSETTER_VEDTAK) {
            return false
        }
        if (Behandlingsstatus.FATTER_VEDTAK == behandling.status &&
            behandling.ansvarligSaksbehandler == ContextService.hentSaksbehandler()) {
            return false
        }
        val inloggetBrukerstilgang = ContextService
                .hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig, "henter behandling")

        val tilganger = inloggetBrukerstilgang.tilganger

        val behandlerRolle =
                if (tilganger.containsKey(Tilgangskontrollsfagsystem.SYSTEM_TILGANG)) Behandlerrolle.SYSTEM
                else tilganger[Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)]

        return when (behandlerRolle) {
            Behandlerrolle.VEILEDER -> false
            Behandlerrolle.SAKSBEHANDLER -> (Behandlingsstatus.UTREDES == behandling.status)
            Behandlerrolle.BESLUTTER, Behandlerrolle.SYSTEM -> true
            else -> false
        }
    }

}
