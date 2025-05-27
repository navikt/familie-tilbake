package no.nav.tilbakekreving.entities

import java.util.UUID

data class VilkårsvurderingsperiodeEntity(
    val id: UUID,
    val periode: DatoperiodeEntity,
    val begrunnelseForTilbakekreving: String?,
    val vurdering: VurderingEntity,
)
