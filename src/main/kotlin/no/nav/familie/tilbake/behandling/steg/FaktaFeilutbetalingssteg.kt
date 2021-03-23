package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FaktaFeilutbetalingssteg(val behandlingskontrollService: BehandlingskontrollService,
                               val faktaFeilutbetalingService: FaktaFeilutbetalingService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)


    @Transactional
    override fun utførSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FAKTA} steg")
        val behandlingsstegFaktaDto: BehandlingsstegFaktaDto = behandlingsstegDto as BehandlingsstegFaktaDto
        faktaFeilutbetalingService.lagreFaktaomfeilutbetaling(behandlingId, behandlingsstegFaktaDto)

        if (faktaFeilutbetalingService.hentAktivFaktaOmFeilutbetaling(behandlingId) != null) {
            behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                     Behandlingsstegsinfo(Behandlingssteg.FAKTA,
                                                                                          Behandlingsstegstatus.UTFØRT))
            behandlingskontrollService.fortsettBehandling(behandlingId)
        }
    }

    @Transactional
    override fun gjenopptaSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.FAKTA} steg")
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(Behandlingssteg.FAKTA,
                                                                                      Behandlingsstegstatus.KLAR))
    }


    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.FAKTA
    }
}
