package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvBrukerBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.KanUnnlates4xRettsgebyrBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ReduksjonSærligeGrunnerBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider

class ForårsaketAvBrukerBuilders<BuiltForårsaketAvBruker>(
    private val provider: VilkårsvurderingProvider<BuiltForårsaketAvBruker, *>,
) {
    fun uaktsomt(
        unnlates: KanUnnlates4xRettsgebyrBuilder = skalIkkeUnnlates(),
        reduksjon: ReduksjonSærligeGrunnerBuilder = ingenReduksjon(),
    ) = ForårsaketAvBrukerBuilder.Uaktsomt<BuiltForårsaketAvBruker>(unnlates, reduksjon).build(provider)

    fun grovtUaktsomt(
        unnlates: KanUnnlates4xRettsgebyrBuilder = skalIkkeUnnlates(),
        reduksjon: ReduksjonSærligeGrunnerBuilder = ingenReduksjon(),
    ) = ForårsaketAvBrukerBuilder.GrovtUaktsomt<BuiltForårsaketAvBruker>(unnlates, reduksjon).build(provider)

    fun medForsett() = ForårsaketAvBrukerBuilder.Forsettelig<BuiltForårsaketAvBruker>().build(provider)
}
