package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable

@Serializable
data class VilkårsvurderingsperiodeEntity(
    val id: String,
    val periode: DatoperiodeEntity,
    val begrunnelseForTilbakekreving: String?,
    val vurdering: VurderingEntity,
)
