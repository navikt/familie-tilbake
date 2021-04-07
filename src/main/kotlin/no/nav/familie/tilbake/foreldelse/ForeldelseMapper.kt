package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.api.dto.ForeldelsesperiodeDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.api.dto.VurdertForeldelseDto
import no.nav.familie.tilbake.api.dto.VurdertForeldelsesperiodeDto
import no.nav.familie.tilbake.beregning.KravgrunnlagsberegningService
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.faktaomfeilutbetaling.LogiskPeriode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import java.math.RoundingMode
import java.util.UUID

object ForeldelseMapper {

    fun tilRespons(logiskePerioder: List<LogiskPeriode>,
                   kravgrunnlag431: Kravgrunnlag431,
                   vurdertForeldelse: VurdertForeldelse?): VurdertForeldelseDto {
        val vurdertForeldelsesperioder: List<VurdertForeldelsesperiodeDto> = vurdertForeldelse?.foreldelsesperioder?.map {
            VurdertForeldelsesperiodeDto(periode = PeriodeDto(it.periode),
                                         feilutbetaltBeløp = KravgrunnlagsberegningService
                                                 .beregnFeilutbetaltBeløp(kravgrunnlag431,
                                                                          it.periode).setScale(0, RoundingMode.HALF_UP),
                                         begrunnelse = it.begrunnelse,
                                         foreldelsesvurderingstype = it.foreldelsesvurderingstype,
                                         foreldelsesfrist = it.foreldelsesfrist,
                                         oppdagelsesdato = it.oppdagelsesdato)
        }
                                                                             ?: logiskePerioder.map {
                                                                                 VurdertForeldelsesperiodeDto(
                                                                                         periode = PeriodeDto(it.periode),
                                                                                         feilutbetaltBeløp = it.feilutbetaltBeløp)
                                                                               }

        return VurdertForeldelseDto(foreldetPerioder = vurdertForeldelsesperioder)
    }

    fun tilDomene(behandlingId: UUID, vurdertForeldetPerioder: List<ForeldelsesperiodeDto>): VurdertForeldelse {
        val foreldelsesperioder: Set<Foreldelsesperiode> = vurdertForeldetPerioder.map {
            Foreldelsesperiode(periode = Periode(it.periode.fom, it.periode.tom),
                               foreldelsesvurderingstype = it.foreldelsesvurderingstype,
                               begrunnelse = it.begrunnelse,
                               foreldelsesfrist = it.foreldelsesfrist,
                               oppdagelsesdato = it.oppdagelsesdato)
        }.toSet()
        return VurdertForeldelse(behandlingId = behandlingId, foreldelsesperioder = foreldelsesperioder)
    }

}
