package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.test.vilkårsvurdering.AktsomhetBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvNavBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider
import java.math.BigDecimal

class ForårsaketAvNavBuilders<BuiltForårsaketAvNav>(private val provider: VilkårsvurderingProvider<*, BuiltForårsaketAvNav>) {
    fun godTro(
        beløpIBehold: BigDecimal? = null,
    ) = ForårsaketAvNavBuilder.GodTroBuilder<BuiltForårsaketAvNav>(beløpIBehold).build(provider)

    fun burdeForstått(
        aktsomhet: AktsomhetBuilder = uaktsomt(),
    ) = ForårsaketAvNavBuilder.BurdeForstått<BuiltForårsaketAvNav>(aktsomhet).build(provider)

    fun forstod(
        aktsomhet: AktsomhetBuilder = grovtUaktsomt(),
    ) = ForårsaketAvNavBuilder.Forstod<BuiltForårsaketAvNav>(aktsomhet).build(provider)
}
