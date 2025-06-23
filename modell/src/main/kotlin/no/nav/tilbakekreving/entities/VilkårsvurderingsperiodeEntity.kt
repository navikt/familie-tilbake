package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg.Vilkårsvurderingsperiode
import java.util.UUID

data class VilkårsvurderingsperiodeEntity(
    val id: UUID,
    val periode: DatoperiodeEntity,
    val begrunnelseForTilbakekreving: String?,
    val vurdering: VurderingEntity,
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
