package no.nav.tilbakekreving.entities

import java.util.UUID

data class VilkårsvurderingstegEntity(
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
    val kravgrunnlagHendelseRef: UUID,
    val foreldelsesteg: ForeldelsestegEntity,
)
