package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandling
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
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
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Fagsystemskonsekvens
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandling.task.OpprettBehandlingManueltTask
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.SendHenleggelsesbrevTask
import no.nav.familie.tilbake.dokumentbestilling.varsel.SendVarselbrevTask
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.task.FinnKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.HentKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.micrometer.TellerService
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
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val fagsakRepository: FagsakRepository,
                        private val taskRepository: TaskRepository,
                        private val brevsporingRepository: BrevsporingRepository,
                        private val kravgrunnlagRepository: KravgrunnlagRepository,
                        private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
                        private val behandlingskontrollService: BehandlingskontrollService,
                        private val behandlingTilstandService: BehandlingTilstandService,
                        private val tellerService: TellerService,
                        private val stegService: StegService,
                        private val oppgaveTaskService: OppgaveTaskService,
                        private val historikkTaskService: HistorikkTaskService,
                        private val tilgangService: TilgangService,
                        @Value("\${OPPRETTELSE_DAGER_BEGRENSNING:6}")
                        private val opprettelseDagerBegrensning: Long,
                        private val integrasjonerClient: IntegrasjonerClient) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun opprettBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        val behandling: Behandling = opprettFørstegangsbehandling(opprettTilbakekrevingRequest)

        //Lag oppgave for behandling
        oppgaveTaskService.opprettOppgaveTask(behandling.id, Oppgavetype.BehandleSak)

        if (opprettTilbakekrevingRequest.faktainfo.tilbakekrevingsvalg === Tilbakekrevingsvalg
                        .OPPRETT_TILBAKEKREVING_MED_VARSEL) {
            val sendVarselbrev = Task(type = SendVarselbrevTask.TYPE,
                                      payload = behandling.id.toString())
            taskRepository.save(sendVarselbrev)
        }

        return behandling
    }

    @Transactional
    fun opprettBehandlingManuellTask(opprettManueltTilbakekrevingRequest: OpprettManueltTilbakekrevingRequest) {
        logger.info("Oppretter OpprettBehandlingManueltTask for request=$opprettManueltTilbakekrevingRequest")
        val properties = Properties().apply {
            setProperty("eksternFagsakId", opprettManueltTilbakekrevingRequest.eksternFagsakId)
            setProperty("ytelsestype", opprettManueltTilbakekrevingRequest.ytelsestype.name)
            setProperty("eksternId", opprettManueltTilbakekrevingRequest.eksternId)
            setProperty("ansvarligSaksbehandler", ContextService.hentSaksbehandler())
        }
        taskRepository.save(Task(type = OpprettBehandlingManueltTask.TYPE,
                                 properties = properties,
                                 payload = ""))
    }

    @Transactional
    fun opprettRevurdering(opprettRevurderingDto: OpprettRevurderingDto): Behandling {
        val originalBehandlingId = opprettRevurderingDto.originalBehandlingId
        logger.info("Oppretter revurdering for behandling $originalBehandlingId")
        val originalBehandling = behandlingRepository.findByIdOrThrow(originalBehandlingId)
        if (!kanRevurderingOpprettes(originalBehandling)) {
            val feilmelding = "Revurdering kan ikke opprettes for behandling $originalBehandlingId. " +
                              "Enten behandlingen er ikke avsluttet med kravgrunnlag eller " +
                              "det finnes allerede en åpen revurdering"
            throw Feil(message = feilmelding, frontendFeilmelding = feilmelding)
        }
        val revurdering = BehandlingMapper.tilDomeneBehandlingRevurdering(originalBehandling, opprettRevurderingDto.årsakstype)
        behandlingRepository.insert(revurdering)

        historikkTaskService.lagHistorikkTask(revurdering.id,
                                              TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET,
                                              Aktør.SAKSBEHANDLER)

        behandlingskontrollService.fortsettBehandling(revurdering.id)
        stegService.håndterSteg(revurdering.id)

        // kjør HentKravgrunnlagTask for å hente kravgrunnlag på nytt fra økonomi
        taskRepository.save(Task(type = HentKravgrunnlagTask.TYPE, payload = revurdering.id.toString()))

        //Lag oppgave for behandling
        oppgaveTaskService.opprettOppgaveTask(revurdering.id, Oppgavetype.BehandleSak)

        return revurdering
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
        val kanRevurderingOpprettes: Boolean =
                tilgangService.tilgangTilÅOppretteRevurdering(fagsak.fagsystem) && kanRevurderingOpprettes(behandling)

        return BehandlingMapper.tilRespons(behandling,
                                           erBehandlingPåVent,
                                           kanBehandlingHenlegges,
                                           kanEndres,
                                           kanRevurderingOpprettes,
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
        oppdaterAnsvarligSaksbehandler(behandlingId)

        behandlingskontrollService.settBehandlingPåVent(behandlingId,
                                                        behandlingPåVentDto.venteårsak,
                                                        behandlingPåVentDto.tidsfrist)

        val beskrivelse = when (behandlingPåVentDto.venteårsak) {
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING -> "Frist er oppdatert pga mottatt tilbakemelding fra bruker"
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG -> "Ny frist satt på bakgrunn av mottatt kravgrunnlag fra økonomi"
            else -> "Frist er oppdatert av saksbehandler ${ContextService.hentSaksbehandler()}"
        }
        oppgaveTaskService.oppdaterOppgaveTask(behandlingId, beskrivelse, behandlingPåVentDto.tidsfrist)
    }

    @Transactional
    fun taBehandlingAvvent(behandlingId: UUID) { // Denne metoden brukes kun for å gjenoppta behandling manuelt av saksbehandler
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)

        if (!behandlingskontrollService.erBehandlingPåVent(behandlingId)) {
            throw Feil(message = "Behandling $behandlingId er ikke på vent, kan ike gjenoppta",
                       frontendFeilmelding = "Behandling $behandlingId er ikke på vent, kan ike gjenoppta",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
        oppdaterAnsvarligSaksbehandler(behandlingId)

        historikkTaskService.lagHistorikkTask(behandling.id,
                                              TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
                                              Aktør.SAKSBEHANDLER)

        stegService.gjenopptaSteg(behandlingId)
        oppgaveTaskService.oppdaterOppgaveTask(behandlingId, "Behandling er tatt av vent", LocalDate.now())

        // oppdaterer oppgave hvis saken er fortsatt på vent
        val aktivStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        if (aktivStegstilstand?.behandlingsstegsstatus == Behandlingsstegstatus.VENTER) {
            oppgaveTaskService.oppdaterOppgaveTask(behandlingId,
                                                   aktivStegstilstand.venteårsak!!.beskrivelse,
                                                   aktivStegstilstand.tidsfrist!!)
        }
    }

    @Transactional
    fun henleggBehandling(behandlingId: UUID, henleggelsesbrevFritekstDto: HenleggelsesbrevFritekstDto) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)

        val behandlingsresultatstype = henleggelsesbrevFritekstDto.behandlingsresultatstype

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

        oppdaterAnsvarligSaksbehandler(behandlingId)
        behandlingTilstandService.opprettSendingAvBehandlingenHenlagt(behandlingId)

        val aktør = when (behandlingsresultatstype) {
            Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
            Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD -> Aktør.VEDTAKSLØSNING
            else -> Aktør.SAKSBEHANDLER
        }
        historikkTaskService.lagHistorikkTask(behandlingId = behandlingId,
                                              historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
                                              aktør = aktør,
                                              beskrivelse = fjernNewlinesFraString(henleggelsesbrevFritekstDto.begrunnelse))

        if (kanSendeHenleggelsesbrev(behandling, behandlingsresultatstype)) {
            taskRepository.save(SendHenleggelsesbrevTask.opprettTask(behandlingId, henleggelsesbrevFritekstDto.fritekst))
        }

        // Ferdigstill oppgave
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId = behandlingId)
        tellerService.tellVedtak(Behandlingsresultatstype.HENLAGT, behandling)
    }

    @Transactional
    fun oppdaterAnsvarligSaksbehandler(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val gammelSaksbehandler = behandling.ansvarligSaksbehandler
        behandlingRepository.update(behandling.copy(ansvarligSaksbehandler = ContextService.hentSaksbehandler()))

        //oppdater saksbehandler på oppgaven også hvis det er ny saksbehandler som behandler saken
        if (gammelSaksbehandler != ContextService.hentSaksbehandler()
            && ContextService.hentSaksbehandler() != Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            oppgaveTaskService.oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId)
        }
    }

    @Transactional
    fun oppdaterFaktainfo(eksternFagsakId: String,
                          ytelsestype: Ytelsestype,
                          eksternId: String,
                          respons: HentFagsystemsbehandling) {
        val behandling = behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId)
                         ?: throw Feil("Det finnes ikke en åpen behandling for " +
                                       "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype")
        val faktainfo = respons.faktainfo
        val fagsystemskonsekvenser = faktainfo.konsekvensForYtelser.map { Fagsystemskonsekvens(konsekvens = it) }.toSet()
        val gammelFagsystemsbehandling = behandling.aktivFagsystemsbehandling.copy(aktiv = false)
        val nyFagsystemsbehandling = Fagsystemsbehandling(eksternId = eksternId,
                                                          årsak = faktainfo.revurderingsårsak,
                                                          resultat = faktainfo.revurderingsresultat,
                // kopier gammel tilbakekrevingsvalg om det ikke finnes i fagsystem
                                                          tilbakekrevingsvalg = faktainfo.tilbakekrevingsvalg
                                                                                ?: gammelFagsystemsbehandling.tilbakekrevingsvalg,
                                                          revurderingsvedtaksdato = respons.revurderingsvedtaksdato,
                                                          konsekvenser = fagsystemskonsekvenser)
        behandlingRepository.update(behandling.copy(fagsystemsbehandling = setOf(gammelFagsystemsbehandling,
                                                                                 nyFagsystemsbehandling)))
    }

    @Transactional
    fun byttBehandlendeEnhet(behandlingId: UUID, byttEnhetDto: ByttEnhetDto) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingAlleredeErAvsluttet(behandling)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        if (fagsak.fagsystem != Fagsystem.BA) {
            throw Feil(message = "Ikke implementert for fagsystem ${fagsak.fagsystem}",
                       frontendFeilmelding = "Ikke implementert for fagsystem: ${fagsak.fagsystem.navn}")
        }

        val enhet = integrasjonerClient.hentNavkontor(byttEnhetDto.enhet)

        behandlingRepository.update(behandling.copy(behandlendeEnhet = byttEnhetDto.enhet, behandlendeEnhetsNavn = enhet.navn))
        oppdaterAnsvarligSaksbehandler(behandlingId)

        historikkTaskService.lagHistorikkTask(behandlingId = behandlingId,
                                              historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET,
                                              aktør = Aktør.SAKSBEHANDLER,
                                              beskrivelse = fjernNewlinesFraString(byttEnhetDto.begrunnelse))

        oppgaveTaskService.oppdaterEnhetOppgaveTask(behandlingId = behandlingId,
                                                    beskrivelse = "Endret tildelt enhet: " + enhet.enhetId,
                                                    enhetId = byttEnhetDto.enhet)
    }

    private fun opprettFørstegangsbehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        val ytelsestype = opprettTilbakekrevingRequest.ytelsestype
        val fagsystem = opprettTilbakekrevingRequest.fagsystem
        validateFagsystem(ytelsestype, fagsystem)
        val eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId
        val eksternId = opprettTilbakekrevingRequest.eksternId
        val erManueltOpprettet = opprettTilbakekrevingRequest.manueltOpprettet

        val ansvarligsaksbehandler = integrasjonerClient.hentSaksbehandler(opprettTilbakekrevingRequest.saksbehandlerIdent)

        logger.info("Oppretter Tilbakekrevingsbehandling for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
                    "og eksternId=$eksternId")
        secureLogger.info("Oppretter Tilbakekrevingsbehandling for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
                          " og personIdent=${opprettTilbakekrevingRequest.personIdent}")

        kanBehandlingOpprettes(ytelsestype, eksternFagsakId, eksternId, erManueltOpprettet)

        // oppretter fagsak hvis det ikke finnes ellers bruker det eksisterende
        val eksisterendeFagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)
        val fagsak = eksisterendeFagsak ?: opprettFagsak(opprettTilbakekrevingRequest, ytelsestype, fagsystem)

        val behandling =
                BehandlingMapper.tilDomeneBehandling(opprettTilbakekrevingRequest, fagsystem, fagsak, ansvarligsaksbehandler)
        behandlingRepository.insert(behandling)

        historikkTaskService.lagHistorikkTask(behandling.id,
                                              TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET,
                                              Aktør.VEDTAKSLØSNING)

        behandlingskontrollService.fortsettBehandling(behandling.id)
        stegService.håndterSteg(behandling.id)

        // kjør FinnGrunnlagTask for å finne og koble grunnlag med behandling
        taskRepository.save(Task(type = FinnKravgrunnlagTask.TYPE, payload = behandling.id.toString()))

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
                                       eksternId: String,
                                       erManueltOpprettet: Boolean) {
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

        // uten kravgrunnlag er det ikke mulig å opprette behandling manuelt
        if (erManueltOpprettet && !økonomiXmlMottattRepository
                        .existsByEksternFagsakIdAndYtelsestypeAndReferanse(eksternFagsakId, ytelsestype, eksternId)) {
            val feilMelding = "Det finnes intet kravgrunnlag for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
                              "og eksternId=$eksternId. Tilbakekrevingsbehandling kan ikke opprettes manuelt."
            throw Feil(message = feilMelding, frontendFeilmelding = feilMelding)
        }

    }

    private fun opprettFagsak(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
                              ytelsestype: Ytelsestype,
                              fagsystem: Fagsystem): Fagsak {
        val bruker = Bruker(ident = opprettTilbakekrevingRequest.personIdent,
                            språkkode = opprettTilbakekrevingRequest.språkkode)
        return fagsakRepository.insert(Fagsak(bruker = bruker,
                                              eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId,
                                              ytelsestype = ytelsestype,
                                              fagsystem = fagsystem))
    }

    private fun kanHenleggeBehandling(behandling: Behandling,
                                      behandlingsresultatstype: Behandlingsresultatstype? = null): Boolean {
        if (Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT == behandlingsresultatstype) {
            return true
        } else if (TILBAKEKREVING == behandling.type) {
            return !behandling.erAvsluttet && (!behandling.manueltOpprettet &&
                                               behandling.opprettetTidspunkt < LocalDate.now()
                                                       .atStartOfDay()
                                                       .minusDays(opprettelseDagerBegrensning))
                   && !kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id)
        }
        return true
    }

    private fun kanSendeHenleggelsesbrev(behandling: Behandling, behandlingsresultatstype: Behandlingsresultatstype): Boolean {
        return when (behandling.type) {
            TILBAKEKREVING -> brevsporingRepository.existsByBehandlingIdAndBrevtypeIn(behandling.id,
                                                                                      setOf(Brevtype.VARSEL,
                                                                                            Brevtype.KORRIGERT_VARSEL))
            REVURDERING_TILBAKEKREVING -> Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_MED_BREV == behandlingsresultatstype
        }
    }

    private fun sjekkOmBehandlingAlleredeErAvsluttet(behandling: Behandling) {
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil("Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    private fun kanBehandlingEndres(behandling: Behandling, fagsystem: Fagsystem): Boolean {
        if (behandling.erSaksbehandlingAvsluttet) {
            return false
        }
        if (Behandlingsstatus.FATTER_VEDTAK == behandling.status &&
            behandling.ansvarligSaksbehandler == ContextService.hentSaksbehandler()) {
            return false
        }

        return when (tilgangService.finnBehandlerrolle(fagsystem)) {
            Behandlerrolle.SAKSBEHANDLER -> (Behandlingsstatus.UTREDES == behandling.status)
            Behandlerrolle.BESLUTTER, Behandlerrolle.SYSTEM -> true
            else -> false
        }
    }

    private fun kanRevurderingOpprettes(behandling: Behandling): Boolean {
        return behandling.erAvsluttet &&
               kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id) &&
               behandlingRepository.finnÅpenTilbakekrevingsrevurdering(behandling.id) == null
    }


    private fun fjernNewlinesFraString(tekst: String): String {
        return tekst
                .replace("\r", "")
                .replace("\n", " ")
    }
}
