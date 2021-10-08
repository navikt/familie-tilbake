package no.nav.familie.tilbake.forvaltning

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

@Service
class ForvaltningService(private val behandlingRepository: BehandlingRepository,
                         private val fagsakRepository: FagsakRepository,
                         private val kravgrunnlagRepository: KravgrunnlagRepository,
                         private val hentKravgrunnlagService: HentKravgrunnlagService,
                         private val økonomiXmlMottattService: ØkonomiXmlMottattService,
                         private val stegService: StegService,
                         private val behandlingskontrollService: BehandlingskontrollService,
                         private val behandlingTilstandService: BehandlingTilstandService,
                         private val historikkTaskService: HistorikkTaskService,
                         private val oppgaveTaskService: OppgaveTaskService,
                         private val tellerService: TellerService,
                         private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun korrigerKravgrunnlag(behandlingId: UUID,
                             kravgrunnlagId: BigInteger) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingErAvsluttet(behandling)
        val hentetKravgrunnlag = hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(kravgrunnlagId,
                                                                                    KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG)

        val kravgrunnlag = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(kravgrunnlagId)
        if (kravgrunnlag != null) {
            kravgrunnlagRepository.update(kravgrunnlag.copy(aktiv = false))
        }
        hentKravgrunnlagService.lagreHentetKravgrunnlag(behandlingId, hentetKravgrunnlag)

        stegService.håndterSteg(behandlingId)
    }

    @Transactional
    fun arkiverMottattKravgrunnlag(mottattXmlId: UUID) {
        logger.info("Arkiverer mottattXml for Id=$mottattXmlId")
        val mottattKravgrunnlag = økonomiXmlMottattService.hentMottattKravgrunnlag(mottattXmlId)
        økonomiXmlMottattService.arkiverMottattXml(mottattKravgrunnlag.melding,
                                                   mottattKravgrunnlag.eksternFagsakId,
                                                   mottattKravgrunnlag.ytelsestype)
        økonomiXmlMottattService.slettMottattXml(mottattXmlId)
    }

    @Transactional
    fun tvingHenleggBehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingErAvsluttet(behandling)

        //oppdaterer behandlingsstegstilstand
        behandlingskontrollService.henleggBehandlingssteg(behandlingId)

        //oppdaterer behandlingsresultat og behandling
        val behandlingsresultat = Behandlingsresultat(type = Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD)
        behandlingRepository.update(behandling.copy(resultater = setOf(behandlingsresultat),
                                                    status = Behandlingsstatus.AVSLUTTET,
                                                    ansvarligSaksbehandler = ContextService.hentSaksbehandler(),
                                                    avsluttetDato = LocalDate.now()))
        behandlingTilstandService.opprettSendingAvBehandlingenHenlagt(behandlingId)

        historikkTaskService.lagHistorikkTask(behandlingId = behandlingId,
                                              historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
                                              aktør = Aktør.SAKSBEHANDLER,
                                              beskrivelse = "")
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId)
        tellerService.tellVedtak(Behandlingsresultatstype.HENLAGT, behandling)
    }

    @Transactional
    fun hentFagsystemsbehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingErAvsluttet(behandling)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val aktivFagsystemsbehandling = behandling.aktivFagsystemsbehandling
        val sendtRequest =
                hentFagsystemsbehandlingService.hentFagsystemsbehandlingRequestSendt(fagsak.eksternFagsakId,
                                                                                     fagsak.ytelsestype,
                                                                                     aktivFagsystemsbehandling.eksternId)
        // fjern eksisterende sendte request slik at ny request kan sendes
        if (sendtRequest != null) {
            hentFagsystemsbehandlingService.fjernHentFagsystemsbehandlingRequest(sendtRequest.id)
        }
        hentFagsystemsbehandlingService.sendHentFagsystemsbehandlingRequest(fagsak.eksternFagsakId,
                                                                            fagsak.ytelsestype,
                                                                            aktivFagsystemsbehandling.eksternId)
    }

    private fun sjekkOmBehandlingErAvsluttet(behandling: Behandling) {
        if (behandling.erAvsluttet) {
            throw Feil("Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }
}