package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.totrinn.TotrinnService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Fattevedtakssteg(private val behandlingskontrollService: BehandlingskontrollService,
                       private val totrinnService: TotrinnService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FATTE_VEDTAK} steg")
    }

    @Transactional
    override fun utførSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FATTE_VEDTAK} steg")
        // step1: oppdater ansvarligBeslutter
        totrinnService.oppdaterAnsvarligBeslutter(behandlingId)

        // step2: lagre totrinnsvurderinger
        val fatteVedtaksstegDto = behandlingsstegDto as BehandlingsstegFatteVedtaksstegDto
        totrinnService.lagreTotrinnsvurderinger(behandlingId, fatteVedtaksstegDto.totrinnsvurderinger)

        // step3a: flytter behandling tilbake til Foreslå Vedtak om beslutter underkjente noen steg
        val finnesUnderkjenteSteg = fatteVedtaksstegDto.totrinnsvurderinger.any { !it.godkjent }
        if (finnesUnderkjenteSteg) {
            behandlingskontrollService.tilbakehoppBehandlingssteg(behandlingId,
                                                                  Behandlingsstegsinfo(Behandlingssteg.FORESLÅ_VEDTAK,
                                                                                       Behandlingsstegstatus.KLAR))
        } else {
            behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                     Behandlingsstegsinfo(Behandlingssteg.FATTE_VEDTAK,
                                                                                          Behandlingsstegstatus.UTFØRT))
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
