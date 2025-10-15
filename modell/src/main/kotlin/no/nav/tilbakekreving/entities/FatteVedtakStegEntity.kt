package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg.Vurdering
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg.VurdertSteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import java.util.UUID

data class FatteVedtakStegEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val vurderteStegEntities: List<VurdertStegEntity>,
    val ansvarligBeslutter: BehandlerEntity?,
) {
    fun fraEntity(): FatteVedtakSteg {
        return FatteVedtakSteg(
            id = id,
            vurderteSteg = vurderteStegEntities.map { it.fraEntity() },
            _ansvarligBeslutter = ansvarligBeslutter?.fraEntity(),
        )
    }
}

data class VurdertStegEntity(
    val id: UUID,
    val fattevedtakRef: UUID,
    val steg: Behandlingssteg,
    val vurdering: VurdertStegType,
    val begrunnelse: String?,
) {
    fun fraEntity(): VurdertSteg {
        val vurdering = when (vurdering) {
            VurdertStegType.IKKE_VURDERT -> Vurdering.IkkeVurdert
            VurdertStegType.GODKJENT -> Vurdering.Godkjent
            VurdertStegType.UNDERKJENT -> Vurdering.Underkjent(begrunnelse!!)
        }
        return VurdertSteg(
            id = id,
            steg = steg,
            vurdering = vurdering,
        )
    }
}

enum class VurdertStegType {
    IKKE_VURDERT,
    GODKJENT,
    UNDERKJENT,
}
