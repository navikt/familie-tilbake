package no.nav.tilbakekreving.entities

data class ForeldelseperiodeEntity(
    val id: String,
    val periode: DatoperiodeEntity,
    val foreldelsesvurdering: ForeldelsesvurderingEntity,
)
