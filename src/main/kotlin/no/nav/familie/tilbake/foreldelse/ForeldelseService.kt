package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.api.dto.BeregnetPeriodeDto
import no.nav.familie.tilbake.api.dto.BeregnetPerioderDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.api.dto.VurdertForeldelseDto
import no.nav.familie.tilbake.beregning.KravgrunnlagsberegningService
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.faktaomfeilutbetaling.LogiskPeriodeUtil
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
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

        return ForeldelseMapper.tilRespons(feilutbetaltePerioder, vurdertForeldelse)
    }

    @Transactional
    fun lagreVurdertForeldelse(behandlingId: UUID, behandlingsstegForeldelseDto: BehandlingsstegForeldelseDto) {
        // alle familie ytelsene er månedsytelser. Så periode som skal lagres bør innenfor en måned
        validateForeldelsesperioder(behandlingsstegForeldelseDto.foreldetPerioder.map { it.periode })
        foreldelseRepository.insert(ForeldelseMapper.tilDomene(behandlingId,
                                                               behandlingsstegForeldelseDto.foreldetPerioder))
    }

    fun beregnBeløp(behandlingId: UUID, perioder: List<PeriodeDto>): BeregnetPerioderDto {
        // alle familie ytelsene er månedsytelser. Så periode som skal lagres bør innenfor en måned
        validateForeldelsesperioder(perioder)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

        return BeregnetPerioderDto(beregnetPerioder = perioder.map {
            val feilutbetaltBeløp = KravgrunnlagsberegningService.beregnFeilutbetaltBeløp(kravgrunnlag, Periode(it.fom, it.tom))
            BeregnetPeriodeDto(periode = it,
                               feilutbetaltBeløp = feilutbetaltBeløp)
        })

    }

    private fun validateForeldelsesperioder(foreldelsesperioder: List<PeriodeDto>) {
        val perioderSomErMindreEnnEnMåned = foreldelsesperioder.filter {
            it.fom.dayOfMonth != 1 ||
            it.tom.dayOfMonth != YearMonth.from(it.tom).lengthOfMonth()
        }

        if (perioderSomErMindreEnnEnMåned.isNotEmpty()) {
            throw Feil(message = "Periode med ${perioderSomErMindreEnnEnMåned[0]} er mindre enn en måned",
                       frontendFeilmelding = "Periode med ${perioderSomErMindreEnnEnMåned[0]} er mindre enn en måned",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

    }
}
