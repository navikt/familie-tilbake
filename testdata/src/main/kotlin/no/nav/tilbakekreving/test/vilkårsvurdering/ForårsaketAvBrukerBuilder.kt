package no.nav.tilbakekreving.test.vilkårsvurdering

sealed interface ForårsaketAvBrukerBuilder<T> {
    fun build(builder: VilkårsvurderingProvider<T, *>): T

    @ConsistentCopyVisibility
    data class Uaktsomt<T> internal constructor(
        var unnlates: KanUnnlates4xRettsgebyrBuilder,
        var reduksjon: ReduksjonSærligeGrunnerBuilder,
    ) : ForårsaketAvBrukerBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<T, *>): T {
            return builder.build(this)
        }
    }

    @ConsistentCopyVisibility
    data class GrovtUaktsomt<T> internal constructor(
        var unnlates: KanUnnlates4xRettsgebyrBuilder,
        var reduksjon: ReduksjonSærligeGrunnerBuilder,
    ) : ForårsaketAvBrukerBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<T, *>): T {
            return builder.build(this)
        }
    }

    class Forsettelig<T> : ForårsaketAvBrukerBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<T, *>): T {
            return builder.build(this)
        }
    }
}
