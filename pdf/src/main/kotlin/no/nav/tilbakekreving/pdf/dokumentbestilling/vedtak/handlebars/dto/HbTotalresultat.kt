package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto

import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import java.math.BigDecimal

data class HbTotalresultat(
    val hovedresultat: Vedtaksresultat,
    val totaltTilbakekrevesBeløp: BigDecimal,
    val totaltTilbakekrevesBeløpMedRenter: BigDecimal,
    val totaltTilbakekrevesBeløpMedRenterUtenSkatt: BigDecimal,
    val totaltRentebeløp: BigDecimal,
) {
    val harSkattetrekk = totaltTilbakekrevesBeløpMedRenterUtenSkatt < totaltTilbakekrevesBeløpMedRenter
}
