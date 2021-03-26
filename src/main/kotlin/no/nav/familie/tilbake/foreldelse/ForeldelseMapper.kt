package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.api.dto.ForeldelsesperiodeDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.api.dto.VurdertForeldelseDto
import no.nav.familie.tilbake.api.dto.VurdertForeldelsesperiodeDto
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.faktaomfeilutbetaling.LogiskPeriode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import java.util.UUID

object ForeldelseMapper {

    fun tilRespons(logiskePerioder: List<LogiskPeriode>,
                   vurdertForeldelse: VurdertForeldelse?): VurdertForeldelseDto {
        val vurdertForeldelsesperioder = logiskePerioder.map {
            val foreldetPeriode: Foreldelsesperiode? =
                    hentForeldetPeriode(vurdertForeldelse?.foreldelsesperioder, it)
            VurdertForeldelsesperiodeDto(periode = PeriodeDto(it.periode),
                                         feilutbetaltBeløp = it.feilutbetaltBeløp,
                                         begrunnelse = foreldetPeriode?.begrunnelse,
                                         foreldelsesvurderingstype = foreldetPeriode?.foreldelsesvurderingstype,
                                         foreldelsesfrist = foreldetPeriode?.foreldelsesfrist,
                                         oppdagelsesdato = foreldetPeriode?.oppdagelsesdato)
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

    private fun hentForeldetPeriode(foreldetPerioder: Set<Foreldelsesperiode>?,
                                    logiskePeriode: LogiskPeriode): Foreldelsesperiode? {
        return foreldetPerioder?.filter { logiskePeriode.periode == it.periode }?.get(0)
    }
}
