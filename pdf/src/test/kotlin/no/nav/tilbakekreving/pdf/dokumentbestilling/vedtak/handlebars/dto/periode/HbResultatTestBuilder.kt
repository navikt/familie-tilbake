package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode

import java.math.BigDecimal

object HbResultatTestBuilder {
    fun forTilbakekrevesBeløp(tilbakekrevesBeløp: Int): HbResultat = forTilbakekrevesBeløpOgRenter(tilbakekrevesBeløp, 0)

    fun forTilbakekrevesBeløpOgRenter(
        tilbakekrevesBeløp: Int,
        renter: Int,
    ): HbResultat =
        HbResultat(
            tilbakekrevesBeløp = BigDecimal.valueOf(tilbakekrevesBeløp.toLong()),
            rentebeløp = BigDecimal.valueOf(renter.toLong()),
            tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal.valueOf(tilbakekrevesBeløp.toLong()),
        )
}
