package no.nav.familie.tilbake.totrinn

import no.nav.familie.tilbake.api.dto.TotrinnsvurderingDto
import no.nav.familie.tilbake.api.dto.VurdertTotrinnDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.totrinn.domain.Totrinnsvurdering
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TotrinnService(val behandlingRepository: BehandlingRepository,
                     val behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository,
                     val totrinnsvurderingRepository: TotrinnsvurderingRepository) {

    @Transactional(readOnly = true)
    fun hentTotrinnsvurderinger(behandlingId: UUID): TotrinnsvurderingDto {
        val totrinnsvurderinger = totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        return TotrinnMapper.tilRespons(totrinnsvurderinger, behandlingsstegstilstand)
    }

    @Transactional
    fun lagreTotrinnsvurderinger(behandlingId: UUID, totrinnsvurderinger: List<VurdertTotrinnDto>) {
        // deaktiver eksisterende totrinnsvurderinger
        val eksisterendeTotrinnsvurdering = totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        eksisterendeTotrinnsvurdering.forEach { totrinnsvurderingRepository.update(it.copy(aktiv = false)) }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        totrinnsvurderinger.filter { finnOmStegKanBesluttes(it.behandlingssteg, behandlingsstegstilstand) }
                .forEach {
                    totrinnsvurderingRepository.insert(Totrinnsvurdering(behandlingId = behandlingId,
                                                                         behandlingssteg = it.behandlingssteg,
                                                                         godkjent = it.godkjent,
                                                                         begrunnelse = it.begrunnelse))
                }
    }

    @Transactional
    fun oppdaterAnsvarligBeslutter(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(ansvarligBeslutter = ContextService.hentSaksbehandler()))
    }

    private fun finnOmStegKanBesluttes(behandlingssteg: Behandlingssteg,
                                       behandlingsstegstilstand: List<Behandlingsstegstilstand>): Boolean {
        return behandlingsstegstilstand.any {
            behandlingssteg == it.behandlingssteg &&
            it.behandlingssteg.kanBesluttes &&
            it.behandlingsstegsstatus != Behandlingsstegstatus.AUTOUTFØRT
        }
    }


}
