package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.test.vilkårsvurdering.AktsomhetBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.BeløpIBeholdBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvNavBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.KanUnnlates4xRettsgebyrBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ReduksjonSærligeGrunnerBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider

class ForårsaketAvNavBuilders<BuiltForårsaketAvNav>(private val provider: VilkårsvurderingProvider<*, BuiltForårsaketAvNav>) {
    fun godTro(
        beløpIBehold: BeløpIBeholdBuilder = ingenting(),
        kanUnnlates: KanUnnlates4xRettsgebyrBuilder? = null,
        reduksjon: ReduksjonSærligeGrunnerBuilder? = null,
    ) = ForårsaketAvNavBuilder.GodTroBuilder<BuiltForårsaketAvNav>(beløpIBehold, kanUnnlates, reduksjon).build(provider)

    fun burdeForstått(
        aktsomhet: AktsomhetBuilder = uaktsomt(),
    ) = ForårsaketAvNavBuilder.BurdeForstått<BuiltForårsaketAvNav>(aktsomhet).build(provider)

    fun forstod(
        aktsomhet: AktsomhetBuilder = grovtUaktsomt(),
    ) = ForårsaketAvNavBuilder.Forstod<BuiltForårsaketAvNav>(aktsomhet).build(provider)
}
