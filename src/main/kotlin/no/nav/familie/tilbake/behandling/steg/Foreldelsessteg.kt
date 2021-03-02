package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegMedStatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class Foreldelsessteg(val behandlingskontrollService: BehandlingskontrollService,
                      val kravgrunnlagRepository: KravgrunnlagRepository) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FORELDELSE} steg")
        if (!harGrunnlagForeldetPeriode(behandlingId)) {
            behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                     BehandlingsstegMedStatus(Behandlingssteg.FORELDELSE,
                                                                                              Behandlingsstegstatus.AUTOUTFØRT))
        }

        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    private fun harGrunnlagForeldetPeriode(behandlingId: UUID): Boolean {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        return kravgrunnlag.perioder.any { it.periode.fom.isBefore(LocalDate.now().minusMonths(FORELDELSE_ANTALL_MÅNED)) }
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.FORELDELSE
    }

    companion object {

        val FORELDELSE_ANTALL_MÅNED = 30L
    }
}
