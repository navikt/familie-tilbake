package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvNavBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider

class ForårsaketAvNavBuilders<BuiltForårsaketAvNav>(private val provider: VilkårsvurderingProvider<*, BuiltForårsaketAvNav>) {
    fun godTro(callback: ForårsaketAvNavBuilder.GodTroBuilder<BuiltForårsaketAvNav>.() -> Unit = {}): BuiltForårsaketAvNav {
        return ForårsaketAvNavBuilder.GodTroBuilder<BuiltForårsaketAvNav>()
            .apply { callback() }
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
