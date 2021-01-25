package no.nav.familie.tilbake.domain

enum class Vurderingspunktsdefinisjon(val behandlingsstegstype: Behandlingsstegstype,
                                      val vurderingspunktstype: String,
                                      val navn: String,
                                      val beskrivelse: String?) {

    VARSELSTEG_INN(Behandlingsstegstype.VARSEL_OM_TILBAKEKREVING, "INN", "Varsel om tilbakekreving - Inngang", null),
    VARSELSTEG_UT(Behandlingsstegstype.VARSEL_OM_TILBAKEKREVING, "UT", "Varsel om tilbakekreving - Utgang", null),
    FORVEDSTEG_INN(Behandlingsstegstype.FORESLÅ_VEDTAK, "INN", "Foreslå vedtak - Inngang", null),
    FORVEDSTEG_UT(Behandlingsstegstype.FORESLÅ_VEDTAK, "UT", "Foreslå vedtak - Utgang", null),
    FVEDSTEG_INN(Behandlingsstegstype.FATTE_VEDTAK, "INN", "Fatter vedtak - Inngang", null),
    FVEDSTEG_UT(Behandlingsstegstype.FATTE_VEDTAK, "UT", "Fatter vedtak - Utgang", null),
    IVEDSTEG_INN(Behandlingsstegstype.IVERKSETT_VEDTAK, "INN", "Iverksett vedtak - Inngang", null),
    IVEDSTEG_UT(Behandlingsstegstype.IVERKSETT_VEDTAK, "UT", "Iverksett vedtak - Utgang", null),
    VTILBSTEG_INN(Behandlingsstegstype.VURDER_TILBAKEKREVING, "INN", "Vurder tilbakekreving - Inngang", null),
    VTILBSTEG_UT(Behandlingsstegstype.VURDER_TILBAKEKREVING, "UT", "Vurder tilbakekreving - Utgang", null),
    TBKGSTEG_INN(Behandlingsstegstype.MOTTA_KRAVGRUNNLAG_FRA_ØKONOMI, "INN", "Tilbakekrevingsgrunnlag - Inngang", null),
    VFORELDETSTEG_INN(Behandlingsstegstype.VURDER_FORELDELSE, "INN", "Vurder foreldelse - Inngang", null),
    VFORELDETSTEG_UT(Behandlingsstegstype.VURDER_FORELDELSE, "UT", "Vurder foreldelse - Utgang", null),
    INOPPSTEG_INN(Behandlingsstegstype.INNHENT_OPPLYSNINGER, "INN", "Innhent opplysninger - Inngang", null),
    INOPPSTEG_UT(Behandlingsstegstype.INNHENT_OPPLYSNINGER, "UT", "Innhent opplysninger - Utgang", null),
    TBKGSTEG_UT(Behandlingsstegstype.MOTTA_KRAVGRUNNLAG_FRA_ØKONOMI, "UT", "Tilbakekrevingsgrunnlag - Utgang", null),
    FAKTFEILUTSTEG_INN(Behandlingsstegstype.FAKTA_OM_FEILUTBETALING, "INN", "Faktafeilutbetaling - Inngang", null),
    FAKTFEILUTSTEG_UT(Behandlingsstegstype.FAKTA_OM_FEILUTBETALING, "UT", "Faktafeilutbetaling - Utgang", null),
    FAKTAVERGESTEG_INN(Behandlingsstegstype.FAKTA_OM_VERGE, "INN", "Fakta om verge - Inngang", null),
    FAKTAVERGESTEG_UT(Behandlingsstegstype.FAKTA_OM_VERGE, "UT", "Fakta om verge - Utgang", null);

}