package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.beregning.Kravgrunnlag431Adapter
import no.nav.familie.tilbake.faktaomfeilutbetaling.LogiskPeriode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.tilbakekreving.api.v1.dto.ForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelsesperiodeDto
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import java.math.RoundingMode
import java.util.UUID

object ForeldelseMapper {
    fun tilRespons(
        logiskePerioder: List<LogiskPeriode>,
        kravgrunnlag431: Kravgrunnlag431,
        vurdertForeldelse: VurdertForeldelse?,
    ): VurdertForeldelseDto {
        val vurdertForeldelsesperioder: List<VurdertForeldelsesperiodeDto> =
            vurdertForeldelse?.foreldelsesperioder?.map {
                VurdertForeldelsesperiodeDto(
                    periode = it.periode.toDatoperiode(),
                    feilutbetaltBeløp = Kravgrunnlag431Adapter(kravgrunnlag431).feilutbetaltBeløp(it.periode.toDatoperiode()),
                    begrunnelse = it.begrunnelse,
                    foreldelsesvurderingstype = it.foreldelsesvurderingstype,
                    foreldelsesfrist = it.foreldelsesfrist,
                    oppdagelsesdato = it.oppdagelsesdato,
                )
            } ?: logiskePerioder.map {
                VurdertForeldelsesperiodeDto(
                    periode = it.periode.toDatoperiode(),
                    feilutbetaltBeløp = it.feilutbetaltBeløp.setScale(0, RoundingMode.HALF_UP),
                )
            }

        return VurdertForeldelseDto(foreldetPerioder = vurdertForeldelsesperioder)
    }

    fun tilDomene(
        behandlingId: UUID,
        vurdertForeldetPerioder: List<ForeldelsesperiodeDto>,
    ): VurdertForeldelse {
        val foreldelsesperioder: Set<Foreldelsesperiode> =
            vurdertForeldetPerioder
                .map {
                    Foreldelsesperiode(
                        periode = Månedsperiode(it.periode.fom, it.periode.tom),
                        foreldelsesvurderingstype = it.foreldelsesvurderingstype,
                        begrunnelse = it.begrunnelse,
                        foreldelsesfrist = it.foreldelsesfrist,
                        oppdagelsesdato = it.oppdagelsesdato,
                    )
                }.toSet()
        return VurdertForeldelse(behandlingId = behandlingId, foreldelsesperioder = foreldelsesperioder)
    }
}
