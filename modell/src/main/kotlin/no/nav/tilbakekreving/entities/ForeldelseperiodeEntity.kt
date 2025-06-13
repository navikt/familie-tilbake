package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg.Foreldelseperiode
import java.util.UUID

data class ForeldelseperiodeEntity(
    val id: UUID,
    val periode: DatoperiodeEntity,
    val foreldelsesvurdering: ForeldelsesvurderingEntity,
) {
    fun fraEntity(): Foreldelseperiode =
        Foreldelseperiode(
            id = id,
            periode = periode.fraEntity(),
            _vurdering = foreldelsesvurdering.fraEntity(),
        )
}
