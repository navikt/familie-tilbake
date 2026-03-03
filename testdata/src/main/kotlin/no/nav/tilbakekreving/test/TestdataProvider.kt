package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.test.vilkårsvurdering.KanUnnlates4xRettsgebyrBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider

interface TestdataProvider<
    BuiltForårsaketAvBruker,
    BuiltForårsaketAvNav,
    Provider : VilkårsvurderingProvider<BuiltForårsaketAvBruker, BuiltForårsaketAvNav>,
> {
    val provider: Provider

    fun forårsaketAvNav(): ForårsaketAvNavBuilders<BuiltForårsaketAvNav> {
        return ForårsaketAvNavBuilders(provider)
    }

    fun forårsaketAvBruker(): ForårsaketAvBrukerBuilders<BuiltForårsaketAvBruker> {
        return ForårsaketAvBrukerBuilders(provider)
    }

    fun skalUnnlates(): KanUnnlates4xRettsgebyrBuilder = KanUnnlates4xRettsgebyrBuilder().apply {
        unnlates = true
    }

    fun skalIkkeUnnlates(): KanUnnlates4xRettsgebyrBuilder = KanUnnlates4xRettsgebyrBuilder().apply {
        unnlates = false
    }
}
