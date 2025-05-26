package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

interface Vurderingsperiode<B : Delperiode.Beløp, T : Delperiode<B>> {
    val periode: Datoperiode

    fun beregningsresultat(): Beregningsresultatsperiode

    val delperioder: List<T>
}
