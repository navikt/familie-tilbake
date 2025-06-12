package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable

@Serializable
data class VilkårsvurderingstegEntity(
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
    val kravgrunnlagHendelseRef: String,
    val foreldelsesteg: ForeldelsestegEntity,
)
