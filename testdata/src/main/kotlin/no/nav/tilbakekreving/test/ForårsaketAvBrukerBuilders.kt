package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvBrukerBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider

class ForårsaketAvBrukerBuilders<BuiltForårsaketAvBruker>(
    private val provider: VilkårsvurderingProvider<BuiltForårsaketAvBruker, *>,
) {
    fun uaktsomt(callback: ForårsaketAvBrukerBuilder.Uaktsomt<BuiltForårsaketAvBruker>.() -> Unit = {}): BuiltForårsaketAvBruker {
        return ForårsaketAvBrukerBuilder.Uaktsomt<BuiltForårsaketAvBruker>()
            .apply { callback() }
            .build(provider)
    }

    fun grovtUaktsomt(callback: ForårsaketAvBrukerBuilder.GrovtUaktsomt<BuiltForårsaketAvBruker>.() -> Unit = {}): BuiltForårsaketAvBruker {
        return ForårsaketAvBrukerBuilder.GrovtUaktsomt<BuiltForårsaketAvBruker>()
            .apply { callback() }
            .build(provider)
    }

    fun medForsett(callback: ForårsaketAvBrukerBuilder.Forsettelig<BuiltForårsaketAvBruker>.() -> Unit = {}): BuiltForårsaketAvBruker {
        return ForårsaketAvBrukerBuilder.Forsettelig<BuiltForårsaketAvBruker>()
            .apply { callback() }
            .build(provider)
    }
}
