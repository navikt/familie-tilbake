package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Periode.Beløp
import java.math.BigDecimal

@Serializable
data class BeløpEntity(
    val klassekode: String,
    val klassetype: String,
    val opprinneligUtbetalingsbeløp: String,
    val nyttBeløp: String,
    val tilbakekrevesBeløp: String,
    val skatteprosent: String,
) {
    fun fraEntity(): Beløp {
        return Beløp(
            klassekode = klassekode,
            klassetype = klassetype,
            opprinneligUtbetalingsbeløp = BigDecimal(opprinneligUtbetalingsbeløp),
            nyttBeløp = BigDecimal(nyttBeløp),
            tilbakekrevesBeløp = BigDecimal(tilbakekrevesBeløp),
            skatteprosent = BigDecimal(skatteprosent),
        )
    }
}
