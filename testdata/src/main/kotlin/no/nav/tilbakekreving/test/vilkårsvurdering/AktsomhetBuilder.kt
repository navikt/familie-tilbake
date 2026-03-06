package no.nav.tilbakekreving.test.vilkårsvurdering

sealed class AktsomhetBuilder {
    abstract fun <T> build(builder: VilkårsvurderingValgProvider<*, *, T>): T

    data class Uaktsomt(
        var unnlates: KanUnnlates4xRettsgebyrBuilder = KanUnnlates4xRettsgebyrBuilder(),
    ) : AktsomhetBuilder() {
        override fun <T> build(builder: VilkårsvurderingValgProvider<*, *, T>): T {
            return builder.build(this)
        }
    }

    data class GrovtUaktsomt(
        var reduksjon: ReduksjonSærligeGrunnerBuilder = ReduksjonSærligeGrunnerBuilder(),
    ) : AktsomhetBuilder() {
        override fun <T> build(builder: VilkårsvurderingValgProvider<*, *, T>): T {
            return builder.build(this)
        }
    }

    data object Forsettelig : AktsomhetBuilder() {
        override fun <T> build(builder: VilkårsvurderingValgProvider<*, *, T>): T {
            return builder.build(this)
        }
    }
}
