package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

class ForeldetBeløp(
    klassekode: String,
    periode: Datoperiode,
    beløpTilbakekreves: KravgrunnlagPeriodeAdapter.BeløpTilbakekreves,
) : Delperiode.Beløp(klassekode, periode, beløpTilbakekreves) {
    override fun tilbakekrevesBrutto(): BigDecimal = BigDecimal.ZERO

    override fun skatt(): BigDecimal = BigDecimal.ZERO
}
