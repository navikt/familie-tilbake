package no.nav.tilbakekreving.avstemming

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Avstemmingsrad(
    val avsender: String,
    val vedtakId: String,
    val fnr: String,
    val vedtaksdato: LocalDateTime,
    val fagsakYtelseType: String,
    val tilbakekrevesBruttoUtenRenter: BigDecimal,
    val skatt: BigDecimal,
    val tilbakekrevesNettoUtenRenter: BigDecimal,
    val renter: BigDecimal,
    val erOmgjøringTilIngenTilbakekreving: Boolean,
) {
    fun toCsvString(): String =
        listOf(
            avsender,
            vedtakId,
            fnr,
            vedtaksdato.format(DATOFORMAT),
            fagsakYtelseType,
            format(tilbakekrevesBruttoUtenRenter),
            format(skatt),
            format(tilbakekrevesNettoUtenRenter),
            format(renter),
            formatOmgjøring(erOmgjøringTilIngenTilbakekreving),
        ).joinToString(SKILLETEGN_KOLONNER)

    private fun format(beløp: BigDecimal): String =
        beløp.setScale(0, RoundingMode.UNNECESSARY).toPlainString()

    private fun formatOmgjøring(verdi: Boolean): String =
        if (verdi) "Omgjoring0" else ""

    companion object {
        const val SKILLETEGN_KOLONNER = ";"
        private val DATOFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
