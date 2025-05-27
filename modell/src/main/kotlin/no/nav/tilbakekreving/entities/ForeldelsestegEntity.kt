package no.nav.tilbakekreving.entities

import java.util.UUID

data class ForeldelsestegEntity(
    val vurdertePerioder: List<ForeldelseperiodeEntity>,
    val kravgrunnlagRef: UUID,
)
