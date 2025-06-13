package no.nav.tilbakekreving.entities

data class VilkårsvurderingstegEntity(
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
    val kravgrunnlagHendelseRef: String,
    val foreldelsesteg: ForeldelsesstegEntity,
)
