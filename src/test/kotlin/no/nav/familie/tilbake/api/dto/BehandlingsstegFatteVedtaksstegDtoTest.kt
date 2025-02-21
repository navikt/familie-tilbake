package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg

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
