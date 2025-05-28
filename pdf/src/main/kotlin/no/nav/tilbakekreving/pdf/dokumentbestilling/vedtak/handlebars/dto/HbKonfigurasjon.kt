package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto

import no.nav.tilbakekreving.Rettsgebyr
import java.math.BigDecimal

@Suppress("unused") // Handlebars
class HbKonfigurasjon(
    val klagefristIUker: Int,
) {
    val fireRettsgebyr: BigDecimal = BigDecimal(Rettsgebyr.rettsgebyr) * BigDecimal(4)
}
