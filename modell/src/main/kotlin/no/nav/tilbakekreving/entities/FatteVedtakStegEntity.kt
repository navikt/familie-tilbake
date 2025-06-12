package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable

@Serializable
data class FatteVedtakStegEntity(
    val vurderteStegEntities: List<VurdertStegEntity>,
    val ansvarligBeslutter: BehandlerEntity?,
)

@Serializable
data class VurdertStegEntity(
    val steg: String,
    val vurdering: String,
    val begrunnelse: String?,
)
