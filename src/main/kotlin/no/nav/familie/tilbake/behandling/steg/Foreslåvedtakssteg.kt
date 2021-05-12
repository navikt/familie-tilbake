package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.totrinn.TotrinnService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Foreslåvedtakssteg(val behandlingskontrollService: BehandlingskontrollService,
                         val vedtaksbrevService: VedtaksbrevService,
                         val oppgaveTaskService: OppgaveTaskService,
                         val totrinnService: TotrinnService) : IBehandlingssteg {

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
        flyttBehandlingVidere(behandlingId)

        // lukker BehandleSak oppgave og oppretter GodkjenneVedtak oppgave
        håndterOppgave(behandlingId)
    }

    @Transactional
    override fun gjenopptaSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.FORESLÅ_VEDTAK} steg")
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(Behandlingssteg.FORESLÅ_VEDTAK,
                                                                                      Behandlingsstegstatus.KLAR))
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
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId, oppgavetype)
        oppgaveTaskService.opprettOppgaveTask(behandlingId, Oppgavetype.GodkjenneVedtak)
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.FORESLÅ_VEDTAK
    }
}
