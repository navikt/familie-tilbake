package no.nav.tilbakekreving.entities

data class VilkårsvurderingsperiodeEntity(
    val id: String,
    val periode: DatoperiodeEntity,
    val begrunnelseForTilbakekreving: String?,
    val vurdering: VurderingEntity,
)
