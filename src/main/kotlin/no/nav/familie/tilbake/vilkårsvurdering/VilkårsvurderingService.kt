package no.nav.familie.tilbake.vilkårsvurdering

import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.api.dto.VurdertVilkårsvurderingDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårsvurderingService(val vilkårsvurderingRepository: VilkårsvurderingRepository,
                              val kravgrunnlagRepository: KravgrunnlagRepository,
                              val fagsakRepository: FagsakRepository,
                              val behandlingRepository: BehandlingRepository,
                              val foreldelseService: ForeldelseService,
                              val faktaFeilutbetalingService: FaktaFeilutbetalingService) {

    fun hentVilkårsvurdering(behandlingId: UUID): VurdertVilkårsvurderingDto {
        val faktaOmFeilutbetaling = faktaFeilutbetalingService.hentAktivFaktaOmFeilutbetaling(behandlingId)
                                    ?: throw Feil(message = "Fakta om feilutbetaling finnes ikke for behandling=$behandlingId, " +
                                                            "kan ikke hente vilkårsvurdering")
        val kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val perioder = mutableListOf<Periode>()
        val foreldetPerioder = mutableListOf<Periode>()
        val vurdertForeldelse = foreldelseService.hentAktivVurdertForeldelse(behandlingId)
        if (vurdertForeldelse == null) {
            // fakta perioder
            faktaOmFeilutbetaling.perioder.forEach { perioder.add(it.periode) }
        } else {
            // Ikke foreldet perioder
            vurdertForeldelse.foreldelsesperioder.filter { !it.erForeldet() }
                    .forEach { perioder.add(it.periode) }
            // foreldet perioder
            vurdertForeldelse.foreldelsesperioder.filter { it.erForeldet() }
                    .forEach { foreldetPerioder.add(it.periode) }
        }
        return VilkårsvurderingMapper.tilRespons(vilkårsvurdering = vilkårsvurdering,
                                                 perioder = perioder,
                                                 foreldetPerioder = foreldetPerioder,
                                                 faktaFeilutbetaling = faktaOmFeilutbetaling,
                                                 kravgrunnlag431 = kravgrunnlag431)
    }

    @Transactional
    fun lagreVilkårsvurdering(behandlingId: UUID, behandlingsstegVilkårsvurderingDto: BehandlingsstegVilkårsvurderingDto) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsystem = fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        // Valider request
        VilkårsvurderingValidator.validerVilkårsvurdering(vilkårsvurderingDto = behandlingsstegVilkårsvurderingDto,
                                                          kravgrunnlag431 = kravgrunnlag)
        // filter bort perioder som er foreldet
        val ikkeForeldetPerioder = behandlingsstegVilkårsvurderingDto.vilkårsvurderingsperioder
                .filter { !foreldelseService.erPeriodeForeldet(behandlingId, Periode(it.periode.fom, it.periode.tom)) }
        deaktiverEksisterendeVilkårsvurdering(behandlingId)
        vilkårsvurderingRepository.insert(VilkårsvurderingMapper.tilDomene(behandlingId = behandlingId,
                                                                           vilkårsvurderingsperioder = ikkeForeldetPerioder,
                                                                           fagsystem = fagsystem))
    }

    @Transactional
    fun deaktiverEksisterendeVilkårsvurdering(behandlingId: UUID) {
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)?.copy(aktiv = false)?.let {
            vilkårsvurderingRepository.update(it)
        }
    }
}
