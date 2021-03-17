package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class Varselssteg(val behandlingskontrollService: BehandlingskontrollService,
                  val brevsporingRepository: BrevsporingRepository) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

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

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.VARSEL
    }
}
