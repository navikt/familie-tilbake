package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import java.util.UUID

data class VilkårsvurderingstegEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val vurderinger: List<VilkårsvurderingsperiodeEntity>,
    val trengerNyVurdering: Boolean,
) {
    fun fraEntity(): Vilkårsvurderingsteg {
        val perioderById = vurderinger.associateBy(VilkårsvurderingsperiodeEntity::id)
        val nesteByForrige = vurderinger
            .filter { it.vurdering.forrigePeriodeId != null }
            .groupBy { requireNotNull(it.vurdering.forrigePeriodeId) }

        val originaleVurderinger = vurderinger
            .filter { it.vurdering.forrigePeriodeId == null }
            .sortedBy { it.periode.fom }

        val perioder = buildList {
            originaleVurderinger.forEach { head ->
                val visited = mutableSetOf<UUID>()
                var current = head
                var forrigeVurdering: ForårsaketAvBruker? = null

                while (visited.add(current.id)) {
                    val vurderingForCurrent = if (current.vurdering.vurderingType == VurderingType.KOPIERT_VURDERING) {
                        val forrige = requireNotNull(forrigeVurdering) {
                            "Kopiert vurdering krever forrige vurdering for periodeId=${current.id}"
                        }
                        ForårsaketAvBruker.KopiertVurdering(
                            originalVurdering = forrige.underliggendeVurdering(),
                            forrigePeriodeId = requireNotNull(current.vurdering.forrigePeriodeId),
                        )
                    } else {
                        current.vurdering.fraEntity(null)
                    }

                    add(current.fraEntity(vurderingForCurrent))
                    forrigeVurdering = vurderingForCurrent

                    val nesteKandidater = nesteByForrige[current.id].orEmpty()
                    require(nesteKandidater.size <= 1) {
                        "Forventet lineær kjede, men fant ${nesteKandidater.size} etterfølgere for periodeId=${current.id}"
                    }

                    current = nesteKandidater
                        .singleOrNull()
                        ?.let { perioderById[it.id] }
                        ?: break
                }

                check(current.id in visited) {
                    "Oppdaget syklus i vilkårsvurderingskjede for periodeId=${current.id}"
                }
            }
        }

        return Vilkårsvurderingsteg(
            id = id,
            vurderinger = perioder,
            underkjent = trengerNyVurdering,
        )
    }
}
