package no.nav.familie.tilbake.totrinn

import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.totrinn.domain.Totrinnsresultatsgrunnlag
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TotrinnService(val faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository,
                     val vurdertForeldelseRepository: VurdertForeldelseRepository,
                     val vilkårsvurderingRepository: VilkårsvurderingRepository,
                     val totrinnsresultatsgrunnlagRepository: TotrinnsresultatsgrunnlagRepository) {

    @Transactional
    fun lagTotrinnsresultat(behandlingId: UUID) {
        val faktaOmFeilutbetaling = faktaFeilutbetalingRepository.findFaktaFeilutbetalingByBehandlingIdAndAktivIsTrue(behandlingId)
        val vurdertForeldelse = vurdertForeldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val vurdertVilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

        totrinnsresultatsgrunnlagRepository.insert(
                Totrinnsresultatsgrunnlag(behandlingId = behandlingId,
                                          faktaFeilutbetalingId = faktaOmFeilutbetaling.id,
                                          vurdertForeldelseId = vurdertForeldelse?.id,
                                          vilkårsvurderingId = vurdertVilkårsvurdering?.id))
    }
}
