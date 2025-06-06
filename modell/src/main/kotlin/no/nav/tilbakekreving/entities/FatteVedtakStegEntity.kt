package no.nav.tilbakekreving.entities

data class FatteVedtakStegEntity(
    val vurderteStegEntities: List<VurdertStegEntity>,
    val ansvarligBeslutter: BehandlerEntity?,
)

data class VurdertStegEntity(
    val steg: String,
    val vurdering: String,
    val begrunnelse: String?,
)
