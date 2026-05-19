package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FagsystemToggle
import no.nav.tilbakekreving.FeatureToggles
import no.nav.tilbakekreving.api.v2.fagsystem.ForenkletBehandlingsstatus
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus

enum class BehandlingsstatusModell(
    val gammelFrontendDTO: Behandlingsstatus,
    val relevantForFagsystem: Boolean = true,
) {
    OPPRETTET(
        gammelFrontendDTO = Behandlingsstatus.OPPRETTET,
    ) {
        override fun forenkletStatus(ytelse: Ytelse, features: FeatureToggles): ForenkletBehandlingsstatus {
            return ForenkletBehandlingsstatus.OPPRETTET
        }
    },
    TIL_FORHÅNDSVARSEL(
        gammelFrontendDTO = Behandlingsstatus.UTREDES,
    ) {
        override fun forenkletStatus(ytelse: Ytelse, features: FeatureToggles): ForenkletBehandlingsstatus {
            return when (features[ytelse, FagsystemToggle.ForhåndsvarselBehandlingsstatuser]) {
                true -> ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL
                false -> ForenkletBehandlingsstatus.TIL_BEHANDLING
            }
        }
    },
    TIL_BEHANDLING(
        gammelFrontendDTO = Behandlingsstatus.UTREDES,
    ) {
        override fun forenkletStatus(ytelse: Ytelse, features: FeatureToggles): ForenkletBehandlingsstatus {
            return ForenkletBehandlingsstatus.TIL_BEHANDLING
        }
    },
    FATTER_VEDTAK(
        gammelFrontendDTO = Behandlingsstatus.FATTER_VEDTAK,
    ) {
        override fun forenkletStatus(ytelse: Ytelse, features: FeatureToggles): ForenkletBehandlingsstatus {
            return ForenkletBehandlingsstatus.TIL_GODKJENNING
        }
    },
    JOURNALFØRER_VEDTAK(
        gammelFrontendDTO = Behandlingsstatus.JOURNALFØR_VEDTAK,
        relevantForFagsystem = false,
    ) {
        override fun forenkletStatus(ytelse: Ytelse, features: FeatureToggles): ForenkletBehandlingsstatus {
            return ForenkletBehandlingsstatus.TIL_GODKJENNING
        }
    },
    DISTRIUBERER_VEDTAK(
        gammelFrontendDTO = Behandlingsstatus.DISTRIUBER_VEDTAK,
        relevantForFagsystem = false,
    ) {
        override fun forenkletStatus(ytelse: Ytelse, features: FeatureToggles): ForenkletBehandlingsstatus {
            return ForenkletBehandlingsstatus.TIL_GODKJENNING
        }
    },
    IVERKSETTER_VEDTAK(
        gammelFrontendDTO = Behandlingsstatus.IVERKSETTER_VEDTAK,
        relevantForFagsystem = false,
    ) {
        override fun forenkletStatus(ytelse: Ytelse, features: FeatureToggles): ForenkletBehandlingsstatus {
            return ForenkletBehandlingsstatus.TIL_GODKJENNING
        }
    },
    AVSLUTTET(
        gammelFrontendDTO = Behandlingsstatus.AVSLUTTET,
    ) {
        override fun forenkletStatus(ytelse: Ytelse, features: FeatureToggles): ForenkletBehandlingsstatus {
            return ForenkletBehandlingsstatus.AVSLUTTET
        }
    }, ;

    abstract fun forenkletStatus(ytelse: Ytelse, features: FeatureToggles): ForenkletBehandlingsstatus
}
