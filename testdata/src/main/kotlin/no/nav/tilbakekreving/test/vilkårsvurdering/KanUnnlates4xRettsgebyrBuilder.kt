package no.nav.tilbakekreving.test.vilkårsvurdering

data class KanUnnlates4xRettsgebyrBuilder(
    var unnlates: Boolean = false,
    var reduksjon: ReduksjonSærligeGrunnerBuilder = ReduksjonSærligeGrunnerBuilder(),
) {
    fun <T> build(builder: VilkårsvurderingValgProvider<T, *, *>): T {
        return builder.build(this)
    }
}
