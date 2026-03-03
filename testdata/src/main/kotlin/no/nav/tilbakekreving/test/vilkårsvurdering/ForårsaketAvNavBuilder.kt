package no.nav.tilbakekreving.test.vilkårsvurdering

import java.math.BigDecimal

sealed interface ForårsaketAvNavBuilder<T> {
    fun build(builder: VilkårsvurderingProvider<*, T>): T

    class GodTroBuilder<T>(
        var beløpIBehold: BigDecimal? = null,
    ) : ForårsaketAvNavBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<*, T>): T {
            return builder.build(this)
        }
    }

    class BurdeForstått<T>(
        var aktsomhet: AktsomhetBuilder = AktsomhetBuilder.GrovtUaktsomt(),
    ) : ForårsaketAvNavBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<*, T>): T {
            return builder.build(this)
        }
    }

    class Forstod<T>(
        var aktsomhet: AktsomhetBuilder = AktsomhetBuilder.GrovtUaktsomt(),
    ) : ForårsaketAvNavBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<*, T>): T {
            return builder.build(this)
        }
    }
}
