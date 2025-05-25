package no.nav.tilbakekreving.beregning.modell

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import java.math.BigDecimal

data class Beregningsresultatsperiode(
    val periode: Datoperiode,
    val vurdering: Vurdering? = null,
    val feilutbetaltBeløp: BigDecimal,
    val andelAvBeløp: BigDecimal? = null,
    val renteprosent: BigDecimal? = null,
    val manueltSattTilbakekrevingsbeløp: BigDecimal? = null,
    val tilbakekrevingsbeløpUtenRenter: BigDecimal,
    val rentebeløp: BigDecimal,
    val tilbakekrevingsbeløp: BigDecimal,
    val skattebeløp: BigDecimal,
    val tilbakekrevingsbeløpEtterSkatt: BigDecimal,
    // Rått beløp, ikke justert for ev. trekk
    val utbetaltYtelsesbeløp: BigDecimal,
    val riktigYtelsesbeløp: BigDecimal,
)
