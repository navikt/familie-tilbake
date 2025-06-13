package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg

data class FatteVedtakStegEntity(
    val vurderteStegEntities: List<VurdertStegEntity>,
    val ansvarligBeslutter: BehandlerEntity?,
)

data class VurdertStegEntity(
    val steg: Behandlingssteg,
    val vurdering: String,
    val begrunnelse: String?,
)
