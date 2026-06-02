package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import java.util.UUID

data class VilkårsvurderingstegEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
    val trengerNyVurdering: Boolean,
) {
    fun fraEntity(): Vilkårsvurderingsteg {
        val vurderingerForId = buildMap {
            vurderinger.forEach { vurdering ->
                put(vurdering.id, vurdering.vurdering.fraEntity(this))
            }
        }

        return Vilkårsvurderingsteg(
            id = id,
            vurderinger = vurderinger.map { it.fraEntity(vurderingerForId[it.id]!!) },
            underkjent = trengerNyVurdering,
        )
    }
}
