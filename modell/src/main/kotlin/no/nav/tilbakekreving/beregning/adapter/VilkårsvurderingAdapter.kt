package no.nav.tilbakekreving.beregning.adapter

interface VilkårsvurderingAdapter {
    fun perioder(): Set<VilkårsvurdertPeriodeAdapter>
}
