package no.nav.tilbakekreving

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.builders.VilkårsvurderingBuilderImpl
import no.nav.tilbakekreving.test.TestdataProvider

object ModellTestdata : TestdataProvider<ForårsaketAvBruker.Ja, ForårsaketAvBruker.Nei, VilkårsvurderingBuilderImpl> {
    override val provider: VilkårsvurderingBuilderImpl = VilkårsvurderingBuilderImpl
}
