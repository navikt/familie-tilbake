package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg.Foreldelseperiode
import java.util.UUID

data class ForeldelseperiodeEntity(
    val id: UUID,
    val foreldelsesvurderingRef: UUID,
    val periode: DatoperiodeEntity,
    val foreldelsesvurdering: ForeldelsesvurderingEntity,
    val endringIKravgrunnlag: ForskjellEntity?,
) {
    fun fraEntity(): Foreldelseperiode =
        Foreldelseperiode(
            id = id,
            periode = periode.fraEntity(),
            _vurdering = foreldelsesvurdering.fraEntity(),
            endringIKravgrunnlag = endringIKravgrunnlag?.fraEntity(),
        )
}
