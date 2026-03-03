package no.nav.tilbakekreving.test.vilkårsvurdering

interface VilkårsvurderingProvider<BuiltForårsaketAvBruker, BuiltForårsaketAvNav> {
    fun build(vurdering: ForårsaketAvNavBuilder.GodTroBuilder<BuiltForårsaketAvNav>): BuiltForårsaketAvNav

    fun build(vurdering: ForårsaketAvNavBuilder.BurdeForstått<BuiltForårsaketAvNav>): BuiltForårsaketAvNav

    fun build(vurdering: ForårsaketAvNavBuilder.Forstod<BuiltForårsaketAvNav>): BuiltForårsaketAvNav

    fun build(vurdering: ForårsaketAvBrukerBuilder.Uaktsomt<BuiltForårsaketAvBruker>): BuiltForårsaketAvBruker

    fun build(vurdering: ForårsaketAvBrukerBuilder.GrovtUaktsomt<BuiltForårsaketAvBruker>): BuiltForårsaketAvBruker

    fun build(vurdering: ForårsaketAvBrukerBuilder.Forsettelig<BuiltForårsaketAvBruker>): BuiltForårsaketAvBruker
}
