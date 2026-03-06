package no.nav.tilbakekreving.test

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
}
