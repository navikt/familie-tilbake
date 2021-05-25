package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FaktaFeilutbetalingssteg(val behandlingskontrollService: BehandlingskontrollService,
                               val faktaFeilutbetalingService: FaktaFeilutbetalingService,
                               val historikkTaskService: HistorikkTaskService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandlingId: UUID) {
        // Denne metoden gjør ingenting. Det skrives bare for å unngå feilen når ENDR kravgrunnlag mottas
    }

    @Transactional
    override fun utførSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FAKTA} steg")
        val behandlingsstegFaktaDto: BehandlingsstegFaktaDto = behandlingsstegDto as BehandlingsstegFaktaDto
        faktaFeilutbetalingService.lagreFaktaomfeilutbetaling(behandlingId, behandlingsstegFaktaDto)

        //historikkinnslag
        historikkTaskService.lagHistorikkTask(behandlingId,
                                              TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT,
                                              Aktør.SAKSBEHANDLER)

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

    @EventListener
    fun deaktiverEksisterendeFaktaOmFeilutbetaling(endretKravgrunnlagEvent: EndretKravgrunnlagEvent) {
        faktaFeilutbetalingService.deaktiverEksisterendeFaktaOmFeilutbetaling(behandlingId = endretKravgrunnlagEvent.behandlingId)
    }
}
