package no.nav.tilbakekreving.kontrakter.behandlingskontroll

enum class Venteårsak(
    val defaultVenteTidIUker: Long,
    val beskrivelse: String,
) {
    VENT_PÅ_BRUKERTILBAKEMELDING(3, "Venter på tilbakemelding fra bruker"),
    VENT_PÅ_TILBAKEKREVINGSGRUNNLAG(4, "Venter på kravgrunnlag fra økonomi"),
    AVVENTER_DOKUMENTASJON(0, "Avventer dokumentasjon"),
    UTVIDET_TILSVAR_FRIST(0, "Utvidet tilsvarsfrist"),
    ENDRE_TILKJENT_YTELSE(0, "Mulig endring i tilkjent ytelse"),
    VENT_PÅ_MULIG_MOTREGNING(0, "Mulig motregning med annen ytelse"),
    ;

    companion object {
        fun venterPåBruker(venteårsak: Venteårsak?): Boolean = venteårsak in listOf(VENT_PÅ_BRUKERTILBAKEMELDING, UTVIDET_TILSVAR_FRIST, AVVENTER_DOKUMENTASJON)

        fun venterPåØkonomi(venteårsak: Venteårsak?): Boolean = venteårsak in listOf(VENT_PÅ_TILBAKEKREVINGSGRUNNLAG, VENT_PÅ_MULIG_MOTREGNING)
    }
}
