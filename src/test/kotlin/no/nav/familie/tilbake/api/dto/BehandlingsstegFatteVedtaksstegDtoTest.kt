package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg

class BehandlingsstegFatteVedtaksstegDtoTest {
    companion object {
        fun ny(godkjent: Boolean = true): BehandlingsstegFatteVedtaksstegDto =
            BehandlingsstegFatteVedtaksstegDto(
                listOf(
                    VurdertTotrinnDto(
                        behandlingssteg = Behandlingssteg.FAKTA,
                        godkjent = godkjent,
                        begrunnelse = "fakta totrinn begrunnelse",
                    ),
                    VurdertTotrinnDto(
                        behandlingssteg = Behandlingssteg.FORELDELSE,
                        godkjent = godkjent,
                        begrunnelse = "foreldelse totrinn begrunnelse",
                    ),
                    VurdertTotrinnDto(
                        behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                        godkjent = godkjent,
                        begrunnelse = "vilkårsvurdering totrinn begrunnelse",
                    ),
                    VurdertTotrinnDto(
                        behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                        godkjent = godkjent,
                        begrunnelse = "foreslåvedtak totrinn begrunnelse",
                    ),
                ),
            )
    }
}
