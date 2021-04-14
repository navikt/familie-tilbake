package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class Foreldelsessteg(val kravgrunnlagRepository: KravgrunnlagRepository,
                      val behandlingskontrollService: BehandlingskontrollService,
                      val foreldelseService: ForeldelseService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FORELDELSE} steg")
        if (!harGrunnlagForeldetPeriode(behandlingId)) {
            behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                     Behandlingsstegsinfo(Behandlingssteg.FORELDELSE,
                                                                                          Behandlingsstegstatus.AUTOUTFØRT))
            behandlingskontrollService.fortsettBehandling(behandlingId)
        }
    }

    @Transactional
    override fun utførSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FORELDELSE} steg")
        foreldelseService.lagreVurdertForeldelse(behandlingId, (behandlingsstegDto as BehandlingsstegForeldelseDto))
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(Behandlingssteg.FORELDELSE,
                                                                                      Behandlingsstegstatus.UTFØRT))
        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    @Transactional
    override fun gjenopptaSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.FORELDELSE} steg")
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(Behandlingssteg.FORELDELSE,
                                                                                      Behandlingsstegstatus.KLAR))
    }

    private fun harGrunnlagForeldetPeriode(behandlingId: UUID): Boolean {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        return kravgrunnlag.perioder.any { it.periode.fom.atDay(1) < LocalDate.now().minusMonths(FORELDELSE_ANTALL_MÅNED) }
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.FORELDELSE
    }

    @EventListener
    fun slettVurdertForeldelse(endretKravgrunnlagEvent: EndretKravgrunnlagEvent) {
        foreldelseService.deaktiverVurdertForeldelse(behandlingId = endretKravgrunnlagEvent.behandlingId)
    }

    companion object {

        const val FORELDELSE_ANTALL_MÅNED = 30L
    }
}
