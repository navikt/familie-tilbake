package no.nav.familie.tilbake.foreldelse

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.api.dto.ForeldelsesperiodeDto
import no.nav.familie.tilbake.api.dto.VurdertForeldelseDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.beregning.KravgrunnlagsberegningUtil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.faktaomfeilutbetaling.LogiskPeriodeUtil
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ForeldelseService(
    private val foreldelseRepository: VurdertForeldelseRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun hentVurdertForeldelse(behandlingId: UUID): VurdertForeldelseDto {
        val vurdertForeldelse: VurdertForeldelse? = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        // Faktaperioder kan ikke deles. Så logisk periode er samme som faktaperiode
        val feilutbetaltePerioder =
            LogiskPeriodeUtil
                .utledLogiskPeriode(KravgrunnlagUtil.finnFeilutbetalingPrPeriode(kravgrunnlag))

        return ForeldelseMapper.tilRespons(feilutbetaltePerioder, kravgrunnlag, vurdertForeldelse)
    }

    fun hentAktivVurdertForeldelse(behandlingId: UUID): VurdertForeldelse? {
        return foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
    }

    fun hentAlleForeldelser(behandlingId: UUID): List<VurdertForeldelse> {
        return foreldelseRepository.findByBehandlingId(behandlingId)
    }

    fun erPeriodeForeldet(
        behandlingId: UUID,
        periode: Månedsperiode,
    ): Boolean {
        return hentAktivVurdertForeldelse(behandlingId)?.foreldelsesperioder
            ?.any { periode == it.periode && it.erForeldet() }
            ?: false
    }

    @Transactional
    fun lagreVurdertForeldelse(
        behandlingId: UUID,
        behandlingsstegForeldelseDto: BehandlingsstegForeldelseDto,
    ) {
        // Alle familieytelsene er månedsytelser. Så periode som skal lagres bør være innenfor en måned
        KravgrunnlagsberegningUtil.validatePerioder(behandlingsstegForeldelseDto.foreldetPerioder.map { it.periode })
        val vurdertForeldelse = ForeldelseMapper.tilDomene(behandlingId, behandlingsstegForeldelseDto.foreldetPerioder)

        nullstillVilkårsvurderingForEndringerIForeldelsesperiode(behandlingId, vurdertForeldelse)
        deaktiverEksisterendeVurdertForeldelse(behandlingId)
        foreldelseRepository.insert(vurdertForeldelse)
    }

    @Transactional
    fun lagreFastForeldelseForAutomatiskSaksbehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val foreldetPerioder =
            hentVurdertForeldelse(behandlingId).foreldetPerioder.map {
                ForeldelsesperiodeDto(
                    periode = it.periode,
                    begrunnelse = Constants.hentAutomatiskForeldelsesbegrunnelse(behandling.saksbehandlingstype),
                    foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET,
                )
            }
        foreldelseRepository.insert(ForeldelseMapper.tilDomene(behandlingId, foreldetPerioder))
    }

    @Transactional
    fun deaktiverEksisterendeVurdertForeldelse(behandlingId: UUID) {
        foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)?.copy(aktiv = false)?.let {
            foreldelseRepository.update(it)
        }
    }

    @Transactional
    fun nullstillVilkårsvurderingForEndringerIForeldelsesperiode(
        behandlingId: UUID,
        vurdertForeldelse: VurdertForeldelse,
    ) {
        val eksisterendeVurdertForeldelse = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId) ?: return
        val eksisterendeVurdertForeldelsesperioder = eksisterendeVurdertForeldelse.foreldelsesperioder.map { it.periode }.toSet()
        val nyVurdertForeldelsesperioder = vurdertForeldelse.foreldelsesperioder.map { it.periode }.toSet()
        val endringerIPeriode = eksisterendeVurdertForeldelsesperioder.minus(nyVurdertForeldelsesperioder)

        if (endringerIPeriode.isEmpty()) return
        // Hvis foreldelsesperioder har endret, må saksbehandler behandle vilkårsvurdering på nytt
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)?.copy(aktiv = false)?.let { vilkårsvurdering ->
            vilkårsvurderingRepository.update(vilkårsvurdering)
        }
    }
}
