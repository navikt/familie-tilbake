package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.api.dto.VurdertForeldelseDto
import no.nav.familie.tilbake.beregning.KravgrunnlagsberegningService
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.faktaomfeilutbetaling.LogiskPeriodeUtil
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ForeldelseService(val foreldelseRepository: VurdertForeldelseRepository,
                        val kravgrunnlagRepository: KravgrunnlagRepository) {

    fun hentVurdertForeldelse(behandlingId: UUID): VurdertForeldelseDto {
        val vurdertForeldelse: VurdertForeldelse? = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        // fakta perioder kan ikke deles. Så logiske periode er samme som fakta periode
        val feilutbetaltePerioder = LogiskPeriodeUtil
                .utledLogiskPeriode(KravgrunnlagUtil.finnFeilutbetalingPrPeriode(kravgrunnlag))

        return ForeldelseMapper.tilRespons(feilutbetaltePerioder, kravgrunnlag, vurdertForeldelse)
    }

    fun hentAktivVurdertForeldelse(behandlingId: UUID): VurdertForeldelse? {
        return foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
    }

    fun erPeriodeForeldet(behandlingId: UUID, periode: Periode): Boolean {
        return hentAktivVurdertForeldelse(behandlingId)?.foreldelsesperioder
                       ?.any { periode == it.periode && it.erForeldet() }
               ?: false
    }

    @Transactional
    fun lagreVurdertForeldelse(behandlingId: UUID, behandlingsstegForeldelseDto: BehandlingsstegForeldelseDto) {
        // alle familie ytelsene er månedsytelser. Så periode som skal lagres bør innenfor en måned
        KravgrunnlagsberegningService.validatePerioder(behandlingsstegForeldelseDto.foreldetPerioder.map { it.periode })
        deaktiverEksisterendeVurdertForeldelse(behandlingId)
        foreldelseRepository.insert(ForeldelseMapper.tilDomene(behandlingId,
                                                               behandlingsstegForeldelseDto.foreldetPerioder))
    }

    @Transactional
    fun deaktiverEksisterendeVurdertForeldelse(behandlingId: UUID) {
        foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)?.copy(aktiv = false)?.let {
            foreldelseRepository.update(it)
        }
    }

}
