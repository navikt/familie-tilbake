package no.nav.tilbakekreving.test.vilkårsvurdering

sealed class AktsomhetBuilder {
    abstract fun <T> build(builder: VilkårsvurderingValgProvider<*, *, T>): T

    @ConsistentCopyVisibility
    data class Uaktsomt internal constructor(
        var unnlates: KanUnnlates4xRettsgebyrBuilder,
        var reduksjon: ReduksjonSærligeGrunnerBuilder,
    ) : AktsomhetBuilder() {
        override fun <T> build(builder: VilkårsvurderingValgProvider<*, *, T>): T {
            return builder.build(this)
        }
    }

    @ConsistentCopyVisibility
    data class GrovtUaktsomt internal constructor(
        var unnlates: KanUnnlates4xRettsgebyrBuilder,
        var reduksjon: ReduksjonSærligeGrunnerBuilder,
    ) : AktsomhetBuilder() {
        override fun <T> build(builder: VilkårsvurderingValgProvider<*, *, T>): T {
            return builder.build(this)
        }
    }

    @ConsistentCopyVisibility
    data object Forsettelig : AktsomhetBuilder() {
        override fun <T> build(builder: VilkårsvurderingValgProvider<*, *, T>): T {
            return builder.build(this)
        }
    }
}
