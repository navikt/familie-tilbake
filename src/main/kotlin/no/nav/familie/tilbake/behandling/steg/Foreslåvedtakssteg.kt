package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.totrinn.TotrinnService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class Foreslåvedtakssteg(private val behandlingRepository: BehandlingRepository,
                         private val fagsakRepository: FagsakRepository,
                         private val behandlingskontrollService: BehandlingskontrollService,
                         private val vedtaksbrevService: VedtaksbrevService,
                         private val oppgaveTaskService: OppgaveTaskService,
                         private val totrinnService: TotrinnService,
                         private val historikkTaskService: HistorikkTaskService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FORESLÅ_VEDTAK} steg")
        flyttBehandlingVidere(behandlingId)
    }

    @Transactional
    override fun utførSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FORESLÅ_VEDTAK} steg")
        val foreslåvedtaksstegDto = behandlingsstegDto as BehandlingsstegForeslåVedtaksstegDto
        vedtaksbrevService.lagreFriteksterFraSaksbehandler(behandlingId, foreslåvedtaksstegDto.fritekstavsnitt)

        val fagsystem = fagsakRepository.finnFagsakForBehandlingId(behandlingId).fagsystem
        historikkTaskService.lagHistorikkTask(behandlingId,
                                              TilbakekrevingHistorikkinnslagstype.FORESLÅ_VEDTAK_VURDERT,
                                              Aktør.SAKSBEHANDLER,
                                              fagsystem.name)

        flyttBehandlingVidere(behandlingId)

        // lukker BehandleSak oppgave og oppretter GodkjenneVedtak oppgave
        håndterOppgave(behandlingId)

        historikkTaskService.lagHistorikkTask(behandlingId = behandlingId,
                                              historikkinnslagstype = TilbakekrevingHistorikkinnslagstype
                                                      .BEHANDLING_SENDT_TIL_BESLUTTER,
                                              aktør = Aktør.SAKSBEHANDLER,
                                              fagsystem = fagsystem.name,
                                              triggerTid = LocalDateTime.now().plusSeconds(2))
    }

    @Transactional
    override fun utførStegAutomatisk(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FORESLÅ_VEDTAK} steg og behandler automatisk..")
        val fagsystem = fagsakRepository.finnFagsakForBehandlingId(behandlingId).fagsystem
        historikkTaskService.lagHistorikkTask(behandlingId,
                                              TilbakekrevingHistorikkinnslagstype.FORESLÅ_VEDTAK_VURDERT,
                                              Aktør.VEDTAKSLØSNING,
                                              fagsystem.name)
        flyttBehandlingVidere(behandlingId)

        // lukker BehandleSak oppgave og oppretter GodkjenneVedtak oppgave
        håndterOppgave(behandlingId)

        historikkTaskService.lagHistorikkTask(behandlingId = behandlingId,
                                              historikkinnslagstype = TilbakekrevingHistorikkinnslagstype
                                                      .BEHANDLING_SENDT_TIL_BESLUTTER,
                                              aktør = Aktør.VEDTAKSLØSNING,
                                              fagsystem = fagsystem.name,
                                              triggerTid = LocalDateTime.now().plusSeconds(2))
    }

    @Transactional
    override fun gjenopptaSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.FORESLÅ_VEDTAK} steg")
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(Behandlingssteg.FORESLÅ_VEDTAK,
                                                                                      Behandlingsstegstatus.KLAR))
    }

    @EventListener
    fun deaktiverEksisterendeVilkårsvurdering(endretKravgrunnlagEvent: EndretKravgrunnlagEvent) {
        vedtaksbrevService.deaktiverEksisterendeVedtaksbrevdata(endretKravgrunnlagEvent.behandlingId)
    }

    private fun flyttBehandlingVidere(behandlingId: UUID) {
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(Behandlingssteg.FORESLÅ_VEDTAK,
                                                                                      Behandlingsstegstatus.UTFØRT))
        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    private fun håndterOppgave(behandlingId: UUID) {
        val finnesUnderkjenteSteg = totrinnService.finnesUnderkjenteStegITotrinnsvurdering(behandlingId)
        var oppgavetype = Oppgavetype.BehandleSak
        if (finnesUnderkjenteSteg) {
            oppgavetype = Oppgavetype.BehandleUnderkjentVedtak
        }
        val fagsystem = fagsakRepository.finnFagsakForBehandlingId(behandlingId).fagsystem
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId, fagsystem, oppgavetype.name)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.saksbehandlingstype == Saksbehandlingstype.ORDINÆR) {
            oppgaveTaskService.opprettOppgaveTask(behandlingId, fagsystem, Oppgavetype.GodkjenneVedtak)
        }
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.FORESLÅ_VEDTAK
    }
}
