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
        val vurdertePerioder = buildMap {
            vurderinger.forEach { vurdering ->
                put(vurdering.id, vurdering.fraEntity(this))
            }
        }

        return Vilkårsvurderingsteg(
            id = id,
            vurderinger = vurdertePerioder.values.toList(),
            underkjent = trengerNyVurdering,
        )
    }
}
