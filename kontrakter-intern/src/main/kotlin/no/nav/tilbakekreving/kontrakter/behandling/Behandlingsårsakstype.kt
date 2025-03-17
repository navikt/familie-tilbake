package no.nav.tilbakekreving.kontrakter.behandling

enum class Behandlingsårsakstype(
    val navn: String,
) {
    REVURDERING_KLAGE_NFP("Revurdering NFP omgjør vedtak basert på klage"),
    REVURDERING_KLAGE_KA("Revurdering etter KA-behandlet klage"),
    REVURDERING_OPPLYSNINGER_OM_VILKÅR("Nye opplysninger om vilkårsvurdering"),
    REVURDERING_OPPLYSNINGER_OM_FORELDELSE("Nye opplysninger om foreldelse"),
    REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT("Feilutbetalt beløp helt eller delvis bortfalt"),
}
