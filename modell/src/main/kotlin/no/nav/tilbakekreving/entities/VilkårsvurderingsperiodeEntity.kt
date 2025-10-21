package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg.Vilkårsvurderingsperiode
import java.util.UUID

data class VilkårsvurderingsperiodeEntity(
    val id: UUID,
    val vurderingRef: UUID? = null,
    val periode: DatoperiodeEntity,
    val begrunnelseForTilbakekreving: String?,
    val vurdering: AktsomhetsvurderingEntity,
) {
    fun fraEntity(): Vilkårsvurderingsperiode {
        return Vilkårsvurderingsperiode(
            id = id,
            periode = periode.fraEntity(),
            begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
            _vurdering = vurdering.fraEntity(),
        )
    }
}
