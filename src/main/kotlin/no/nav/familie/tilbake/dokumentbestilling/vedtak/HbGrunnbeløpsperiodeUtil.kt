package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.beregning.Grunnbeløpsperioder
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbGrunnbeløpsperiode

object HbGrunnbeløpsperiodeUtil {

    fun utledGrunnbeløpsperioder(periode: Månedsperiode) =
        Grunnbeløpsperioder.finnGrunnbeløpsperioderForPeriode(periode).sortedBy { it.periode }.map {
            val snitt = it.periode.snitt(periode) ?: error("Finner ikke snitt for $periode og $it")
            HbGrunnbeløpsperiode(snitt.fomDato, snitt.tomDato, it.grunnbeløp.multiply(6.toBigDecimal()))
        }
}