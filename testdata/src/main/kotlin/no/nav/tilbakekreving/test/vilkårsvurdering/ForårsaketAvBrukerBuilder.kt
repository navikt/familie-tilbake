package no.nav.tilbakekreving.test.vilkårsvurdering

sealed interface ForårsaketAvBrukerBuilder<T> {
    fun build(builder: VilkårsvurderingProvider<T, *>): T

    data class Uaktsomt<T>(
        var skalUnnlates: KanUnnlates4xRettsgebyrBuilder = KanUnnlates4xRettsgebyrBuilder(),
    ) : ForårsaketAvBrukerBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<T, *>): T {
            return builder.build(this)
        }
    }

    class GrovtUaktsomt<T>(
        var reduksjon: ReduksjonSærligeGrunnerBuilder = ReduksjonSærligeGrunnerBuilder(),
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
