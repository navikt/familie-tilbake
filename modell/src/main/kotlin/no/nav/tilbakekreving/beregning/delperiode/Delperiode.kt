package no.nav.tilbakekreving.beregning.delperiode

import java.math.BigDecimal
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode

interface Delperiode {
    fun feilutbetalt(): BigDecimal
    fun tilbakekreves(): BigDecimal
    fun beregningsresultat(): Beregningsresultatsperiode
}
