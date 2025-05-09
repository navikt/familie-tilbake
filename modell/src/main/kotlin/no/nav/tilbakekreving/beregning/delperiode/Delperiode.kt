package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode

interface Delperiode {
    fun beregningsresultat(): Beregningsresultatsperiode
}
