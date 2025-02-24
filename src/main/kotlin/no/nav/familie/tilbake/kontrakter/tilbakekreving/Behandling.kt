package no.nav.familie.tilbake.kontrakter.tilbakekreving

import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val opprettetTidspunkt: LocalDateTime,
    val aktiv: Boolean,
    val årsak: Behandlingsårsakstype?,
    val type: Behandlingstype,
    val status: Behandlingsstatus,
    val vedtaksdato: LocalDateTime?,
    val resultat: Behandlingsresultatstype?,
)

enum class Behandlingsårsakstype {
    REVURDERING_KLAGE_NFP,
    REVURDERING_KLAGE_KA,
    REVURDERING_OPPLYSNINGER_OM_VILKÅR,
    REVURDERING_OPPLYSNINGER_OM_FORELDELSE,
    REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT,
}

enum class Behandlingsstatus {
    AVSLUTTET,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    OPPRETTET,
    UTREDES,
}

enum class Behandlingsresultatstype {
    IKKE_FASTSATT,
    INGEN_TILBAKEBETALING,
    DELVIS_TILBAKEBETALING,
    FULL_TILBAKEBETALING,
    HENLAGT,
}
