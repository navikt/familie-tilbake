package no.nav.tilbakekreving.test.vilkårsvurdering

class ReduksjonSærligeGrunnerBuilder(
    var skalReduseres: Boolean = false,
    var reduksjon: Int = 0,
) {
    fun <T> build(builder: VilkårsvurderingValgProvider<*, T, *>): T {
        return builder.build(this)
    }
}
