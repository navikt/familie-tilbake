package no.nav.tilbakekreving.entities

import java.util.UUID

data class ForeldelseperiodeEntity(
    val id: UUID,
    val periode: DatoperiodeEntity,
    val foreldelsesvurdering: ForeldelsesvurderingEntity,
)
