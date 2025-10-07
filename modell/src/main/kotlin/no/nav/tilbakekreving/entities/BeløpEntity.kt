package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Periode.Beløp
import java.math.BigDecimal
import java.util.UUID

data class BeløpEntity(
    val id: UUID,
    val kravgrunnlagPeriodeId: UUID,
    val klassekode: String,
    val klassetype: String,
    val opprinneligUtbetalingsbeløp: BigDecimal,
    val nyttBeløp: BigDecimal,
    val tilbakekrevesBeløp: BigDecimal,
    val skatteprosent: BigDecimal,
) {
    fun fraEntity(): Beløp {
        return Beløp(
            id = id,
            klassekode = klassekode,
            klassetype = klassetype,
            opprinneligUtbetalingsbeløp = opprinneligUtbetalingsbeløp,
            nyttBeløp = nyttBeløp,
            tilbakekrevesBeløp = tilbakekrevesBeløp,
            skatteprosent = skatteprosent,
        )
    }
}
