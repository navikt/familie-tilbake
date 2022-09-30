package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.beregning.Grunnbeløpsperioder
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbGrunnbeløpsperiode

object HbGrunnbeløpsperiodeUtil {

    fun utledGrunnbeløpsperioder(periode: Månedsperiode): List<HbGrunnbeløpsperiode> {
        val perioder = Grunnbeløpsperioder.finnGrunnbeløpsperioderForPeriode(periode).sortedBy { it.periode }
        return perioder.mapIndexed { index, beløpsperiode ->
            val snitt = beløpsperiode.periode.snitt(periode) ?: error("Finner ikke snitt for $periode og $beløpsperiode")
            HbGrunnbeløpsperiode(
                snitt.fomDato,
                snitt.tomDato,
                beløpsperiode.grunnbeløp.multiply(6.toBigDecimal()),
                index == 0,
                index == perioder.size - 1
            )
        }
    }
}
