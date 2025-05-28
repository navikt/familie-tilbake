package no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto

import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.pdf.handlebars.dto.Språkstøtte
import java.math.BigDecimal
import java.time.YearMonth

class Vedleggsdata(
    override val språkkode: Språkkode,
    // Handlebars
    @Suppress("unused")
    val ytelseMedSkatt: Boolean,
    val feilutbetaltePerioder: List<FeilutbetaltPeriode>,
) : Språkstøtte

data class FeilutbetaltPeriode(
    val måned: YearMonth,
    val nyttBeløp: BigDecimal,
    val tidligereUtbetaltBeløp: BigDecimal,
    val feilutbetaltBeløp: BigDecimal,
)
