package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Varselssteg(private val behandlingskontrollService: BehandlingskontrollService,
                  private val historikkTaskService: HistorikkTaskService,
                  private val brevsporingRepository: BrevsporingRepository) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.VARSEL} steg")
        if (brevsporingRepository.existsByBehandlingIdAndBrevtypeIn(behandlingId,
                                                                    setOf(Brevtype.VARSEL, Brevtype.KORRIGERT_VARSEL))) {
            behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                     Behandlingsstegsinfo(Behandlingssteg.VARSEL,
                                                                                          Behandlingsstegstatus.UTFØRT))
            behandlingskontrollService.fortsettBehandling(behandlingId)
        }
    }

    @Transactional
    override fun gjenopptaSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.VARSEL} steg")
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(Behandlingssteg.VARSEL,
                                                                                      Behandlingsstegstatus.UTFØRT))
        behandlingskontrollService.fortsettBehandling(behandlingId)

        historikkTaskService.lagHistorikkTask(behandlingId,
                                              TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
                                              Aktør.SAKSBEHANDLER)
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.VARSEL
    }
}
