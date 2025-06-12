package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable

@Serializable
data class ForeldelseperiodeEntity(
    val id: String,
    val periode: DatoperiodeEntity,
    val foreldelsesvurdering: ForeldelsesvurderingEntity,
)
