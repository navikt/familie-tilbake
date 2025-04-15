package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter

class GammelVilkårsvurderingAdapter(
    private val vilkårsvurdering: Vilkårsvurdering?,
) : VilkårsvurderingAdapter {
    override fun perioder(): Set<VilkårsvurdertPeriodeAdapter> {
        return vilkårsvurdering?.perioder?.map(::VilkårsvurderingsperiodeAdapter)?.toSet() ?: emptySet()
    }
}
