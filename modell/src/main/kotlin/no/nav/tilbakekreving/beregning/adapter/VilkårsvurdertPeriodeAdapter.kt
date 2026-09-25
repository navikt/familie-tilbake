package no.nav.tilbakekreving.beregning.adapter

import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import java.math.BigDecimal

interface VilkårsvurdertPeriodeAdapter {
    fun periode(): Datoperiode

    fun renter(): Boolean

    fun reduksjon(): Reduksjon

    fun vurdering(): Vurdering

    fun beløpIbehold(feilutbetaltBeløp: BigDecimal): BigDecimal?
}
