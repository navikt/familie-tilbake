package no.nav.familie.tilbake.domain

enum class Behandlingsstegstype(val navn: String,
                                val defaultBehandlingsstatus: Behandlingsstatus,
                                val beskrivelse: String) {

    VARSEL_OM_TILBAKEKREVING("Varsel om tilbakekreving",
                             Behandlingsstatus.UTREDES,
                             "Vurdere om varsel om tilbakekreving skal sendes til søker."),
    FORESLÅ_VEDTAK("Foreslå vedtak",
                   Behandlingsstatus.UTREDES,
                   "Totrinnskontroll av behandling. Går rett til fatte vedtak dersom behandlingen ikke krever totrinnskontroll."),
    FATTE_VEDTAK("Fatte Vedtak", Behandlingsstatus.FATTER_VEDTAK, "Fatte vedtak for en behandling."),
    IVERKSETT_VEDTAK("Iverksett Vedtak",
                     Behandlingsstatus.IVERKSETTER_VEDTAK,
                     "Iverksett vedtak fra en behandling.  Forutsetter at et vedtak er fattet"),
    VURDER_TILBAKEKREVING("Vurder tilbakekreving", Behandlingsstatus.UTREDES, "Vurdere om og hva som skal tilbakekreves"),
    MOTTA_KRAVGRUNNLAG_FRA_ØKONOMI("Motta kravgrunnlag fra økonomi",
                                   Behandlingsstatus.UTREDES,
                                   "Mottat kravgrunnlag fra økonomi for tilbakekrevingsbehandling"),
    VURDER_FORELDELSE("Vurder foreldelse", Behandlingsstatus.UTREDES, "Vurder om feilutbetalte perioder er foreldet."),
    INNHENT_OPPLYSNINGER("Innhent opplysninger", Behandlingsstatus.UTREDES, "Innhente opplysninger fra andre systemer."),
    FAKTA_OM_FEILUTBETALING("Fakta om Feilutbetaling", Behandlingsstatus.UTREDES, "Lagre fakta om feilutbetaling"),
    HENT_GRUNNLAG_FRA_ØKONOMI("Hent grunnlag fra økonomi",
                              Behandlingsstatus.UTREDES,
                              "Hent kravgrunnlag fra økonomi for tilbakekrevingsrevurdering"),
    FAKTA_OM_VERGE("Fakta om verge", Behandlingsstatus.UTREDES, "Lagre fakta om verge"),
}