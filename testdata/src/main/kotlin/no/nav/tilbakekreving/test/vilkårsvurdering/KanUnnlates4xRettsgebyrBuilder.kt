package no.nav.tilbakekreving.test.vilkårsvurdering

@ConsistentCopyVisibility
data class KanUnnlates4xRettsgebyrBuilder internal constructor(var unnlates: Boolean) {
    fun <T> build(builder: VilkårsvurderingValgProvider<T, *, *>, reduksjon: ReduksjonSærligeGrunnerBuilder): T {
        return builder.build(this, reduksjon)
    }
}
