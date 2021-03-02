package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegMedStatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MottattGrunnlagssteg(val kravgrunnlagRepository: KravgrunnlagRepository,
                           val behandlingskontrollService: BehandlingskontrollService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)


    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.GRUNNLAG} steg")
        if (kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandlingId)) {
            behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                     BehandlingsstegMedStatus(Behandlingssteg.GRUNNLAG,
                                                                                              Behandlingsstegstatus.UTFØRT))
        }
        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.GRUNNLAG
    }

}
