package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.totrinn.TotrinnService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Fattevedtakssteg(private val behandlingskontrollService: BehandlingskontrollService,
                       private val totrinnService: TotrinnService,
                       private val oppgaveTaskService: OppgaveTaskService,
                       private val historikkTaskService: HistorikkTaskService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FATTE_VEDTAK} steg")
    }

    @Transactional
    override fun utførSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FATTE_VEDTAK} steg")
        // step1: oppdater ansvarligBeslutter
        totrinnService.validerAnsvarligBeslutter(behandlingId)
        totrinnService.oppdaterAnsvarligBeslutter(behandlingId)

        // step2: lagre totrinnsvurderinger
        val fatteVedtaksstegDto = behandlingsstegDto as BehandlingsstegFatteVedtaksstegDto
        totrinnService.lagreTotrinnsvurderinger(behandlingId, fatteVedtaksstegDto.totrinnsvurderinger)

        // step3: lukk Godkjenne vedtak oppgaver
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId, Oppgavetype.GodkjenneVedtak)

        // step4: flytter behandling tilbake til Foreslå Vedtak om beslutter underkjente noen steg
        val finnesUnderkjenteSteg = fatteVedtaksstegDto.totrinnsvurderinger.any { !it.godkjent }
        if (finnesUnderkjenteSteg) {
            behandlingskontrollService.behandleStegPåNytt(behandlingId, Behandlingssteg.FORESLÅ_VEDTAK)

            //historikkinnslag
            historikkTaskService.lagHistorikkTask(behandlingId,
                                                  TilbakekrevingHistorikkinnslagstype.BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER,
                                                  Aktør.BESLUTTER)
            oppgaveTaskService.opprettOppgaveTask(behandlingId, Oppgavetype.BehandleUnderkjentVedtak)
        } else {
            behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                     Behandlingsstegsinfo(Behandlingssteg.FATTE_VEDTAK,
                                                                                          Behandlingsstegstatus.UTFØRT))
            //historikkinnslag
            historikkTaskService.lagHistorikkTask(behandlingId,
                                                  TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET,
                                                  Aktør.BESLUTTER)
        }
        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    @Transactional
    override fun gjenopptaSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.FATTE_VEDTAK} steg")
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(Behandlingssteg.FATTE_VEDTAK,
                                                                                      Behandlingsstegstatus.KLAR))
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.FATTE_VEDTAK
    }

}
