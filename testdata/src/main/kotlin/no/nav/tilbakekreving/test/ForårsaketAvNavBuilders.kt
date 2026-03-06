package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvNavBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider
import java.math.BigDecimal

class ForårsaketAvNavBuilders<BuiltForårsaketAvNav>(private val provider: VilkårsvurderingProvider<*, BuiltForårsaketAvNav>) {
    fun godTro(beløpIBehold: BigDecimal? = null): BuiltForårsaketAvNav {
        return ForårsaketAvNavBuilder.GodTroBuilder<BuiltForårsaketAvNav>()
            .apply {
                this.beløpIBehold = beløpIBehold
            }
            .build(provider)
    }

    fun burdeForstått(callback: ForårsaketAvNavBuilder.BurdeForstått<BuiltForårsaketAvNav>.() -> Unit = {}): BuiltForårsaketAvNav {
        return ForårsaketAvNavBuilder.BurdeForstått<BuiltForårsaketAvNav>()
            .apply { callback() }
            .build(provider)
    }

    fun forstod(callback: ForårsaketAvNavBuilder.Forstod<BuiltForårsaketAvNav>.() -> Unit = {}): BuiltForårsaketAvNav {
        return ForårsaketAvNavBuilder.Forstod<BuiltForårsaketAvNav>()
            .apply { callback() }
            .build(provider)
    }
}
