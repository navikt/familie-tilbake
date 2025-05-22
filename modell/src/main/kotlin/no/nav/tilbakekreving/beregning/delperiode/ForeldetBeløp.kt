package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

class ForeldetBeløp(
    override val klassekode: String,
    override val periode: Datoperiode,
    val beløpTilbakekreves: KravgrunnlagPeriodeAdapter.BeløpTilbakekreves,
) : Delperiode.Beløp {
    override fun tilbakekrevesBrutto(): BigDecimal = BigDecimal.ZERO

    override fun utbetaltYtelsesbeløp(): BigDecimal = beløpTilbakekreves.utbetaltYtelsesbeløp()

    override fun riktigYtelsesbeløp(): BigDecimal = beløpTilbakekreves.riktigYteslesbeløp()

    override fun skatt(): BigDecimal = BigDecimal.ZERO
}
