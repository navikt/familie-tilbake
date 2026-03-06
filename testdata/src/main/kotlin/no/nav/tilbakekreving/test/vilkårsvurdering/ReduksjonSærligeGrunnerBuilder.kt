package no.nav.tilbakekreving.test.vilkårsvurdering

@ConsistentCopyVisibility
data class ReduksjonSærligeGrunnerBuilder internal constructor(
    var skalReduseres: Boolean,
    var reduksjon: Int,
) {
    fun <T> build(builder: VilkårsvurderingValgProvider<*, T, *>): T {
        return builder.build(this)
    }
}
