package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Periode.Beløp
import java.math.BigDecimal

data class BeløpEntity(
    val klassekode: String,
    val klassetype: String,
    val opprinneligUtbetalingsbeløp: BigDecimal,
    val nyttBeløp: BigDecimal,
    val tilbakekrevesBeløp: BigDecimal,
    val skatteprosent: BigDecimal,
) {
    fun tilDomain(): Beløp {
        return Beløp(
            klassekode = klassekode,
            klassetype = klassetype,
            opprinneligUtbetalingsbeløp = opprinneligUtbetalingsbeløp,
            nyttBeløp = nyttBeløp,
            tilbakekrevesBeløp = tilbakekrevesBeløp,
            skatteprosent = skatteprosent,
        )
    }
}
