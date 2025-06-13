package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg

data class FatteVedtakStegEntity(
    val vurderteStegEntities: List<VurdertStegEntity>,
    val ansvarligBeslutter: BehandlerEntity?,
)

data class VurdertStegEntity(
    val steg: Behandlingssteg,
    val vurdering: VurdertStegType,
    val begrunnelse: String?,
)

enum class VurdertStegType {
    IKKE_VURDERT,
    GODKJENT,
    UNDERKJENT,
}
