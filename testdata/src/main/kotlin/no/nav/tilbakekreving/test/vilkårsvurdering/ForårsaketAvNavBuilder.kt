package no.nav.tilbakekreving.test.vilkårsvurdering

import no.nav.tilbakekreving.test.grovtUaktsomt
import java.math.BigDecimal

sealed interface ForårsaketAvNavBuilder<T> {
    fun build(builder: VilkårsvurderingProvider<*, T>): T

    @ConsistentCopyVisibility
    data class GodTroBuilder<T> internal constructor(
        var beløpIBehold: BigDecimal?,
    ) : ForårsaketAvNavBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<*, T>): T {
            return builder.build(this)
        }
    }

    @ConsistentCopyVisibility
    data class BurdeForstått<T> internal constructor(
        var aktsomhet: AktsomhetBuilder,
    ) : ForårsaketAvNavBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<*, T>): T {
            return builder.build(this)
        }
    }

    @ConsistentCopyVisibility
    data class Forstod<T> internal constructor(
        var aktsomhet: AktsomhetBuilder = grovtUaktsomt(),
    ) : ForårsaketAvNavBuilder<T> {
        override fun build(builder: VilkårsvurderingProvider<*, T>): T {
            return builder.build(this)
        }
    }
}
