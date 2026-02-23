package no.nav.tilbakekreving.kontrakter.tilstand

enum class TilbakekrevingTilstand {
    START,
    AVVENTER_KRAVGRUNNLAG,
    AVVENTER_FAGSYSTEMINFO,
    AVVENTER_BRUKERINFO,
    SEND_VARSELBREV,
    TIL_BEHANDLING,
    IVERKSETT_VEDTAK,
    JOURNALFØR_VEDTAK,
    DISTRIUBER_VEDTAK,
    AVSLUTTET,
}
