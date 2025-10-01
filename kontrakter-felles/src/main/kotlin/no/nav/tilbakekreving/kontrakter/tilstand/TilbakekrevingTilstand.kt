package no.nav.tilbakekreving.kontrakter.tilstand

enum class TilbakekrevingTilstand {
    START,
    AVVENTER_KRAVGRUNNLAG,
    AVVENTER_FAGSYSTEMINFO,
    AVVENTER_BRUKERINFO,
    SEND_VARSELBREV,
    IVERKSETT_VEDTAK,
    TIL_BEHANDLING,
    AVSLUTTET,
}
