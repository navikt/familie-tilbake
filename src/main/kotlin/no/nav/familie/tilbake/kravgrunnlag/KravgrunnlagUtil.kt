package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import java.math.BigDecimal
import java.util.Comparator
import java.util.SortedMap

object KravgrunnlagUtil {

    fun finnFeilutbetalingPrPeriode(kravgrunnlag: Kravgrunnlag431): SortedMap<Periode, BigDecimal> {
        val feilutbetalingPrPeriode = mutableMapOf<Periode, BigDecimal>()
        for (kravgrunnlagPeriode432 in kravgrunnlag.perioder) {
            val feilutbetaltBeløp = kravgrunnlagPeriode432.beløp
                    .filter { Klassetype.FEIL === it.klassetype }
                    .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
            if (feilutbetaltBeløp.compareTo(BigDecimal.ZERO) != 0) {
                feilutbetalingPrPeriode[kravgrunnlagPeriode432.periode] = feilutbetaltBeløp
            }
        }
        return feilutbetalingPrPeriode.toSortedMap(Comparator.comparing(Periode::fom).thenComparing(Periode::tom))
    }
}
