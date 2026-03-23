package no.nav.tilbakekreving.kontrakter.behandlingskontroll

import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus

enum class Behandlingssteg(
    val sekvens: Int,
    val kanSaksbehandles: Boolean,
    val kanBesluttes: Boolean,
    val behandlingsstatus: Behandlingsstatus,
    val kortNavn: String,
    private val beskrivelse: String,
) {
    VARSEL(1, false, false, Behandlingsstatus.UTREDES, "forhåndsvarsel", "Vurdere om varsel om tilbakekreving skal sendes til søker"),
    FORHÅNDSVARSEL(1, false, false, Behandlingsstatus.UTREDES, "forhåndsvarsel", "Vurdere om varsel om tilbakekreving skal sendes til søker"),
    GRUNNLAG(2, false, false, Behandlingsstatus.UTREDES, "grunnlag", "Mottat kravgrunnlag fra økonomi for tilbakekrevingsrevurdering"),
    BREVMOTTAKER(3, true, false, Behandlingsstatus.UTREDES, "brevmottaker", "Registrere brevmottakere manuelt. Erstatter Verge-steget"),

    @Deprecated("Erstattes av BREVMOTTAKER")
    VERGE(3, true, false, Behandlingsstatus.UTREDES, "verge", "Fakta om verge"),
    FAKTA(4, true, true, Behandlingsstatus.UTREDES, "fakta", "Fakta om Feilutbetaling"),
    FORELDELSE(5, true, true, Behandlingsstatus.UTREDES, "foreldelse", "Vurder om feilutbetalte perioder er foreldet"),
    VILKÅRSVURDERING(6, true, true, Behandlingsstatus.UTREDES, "vilkår", "Vurdere om og hva som skal tilbakekreves"),
    FORESLÅ_VEDTAK(7, true, true, Behandlingsstatus.UTREDES, "vedtak", "Foreslår vedtak"),
    FATTE_VEDTAK(8, true, false, Behandlingsstatus.FATTER_VEDTAK, "beslutning", "Fatter vedtak"),
    IVERKSETT_VEDTAK(
        9,
        false,
        false,
        Behandlingsstatus.IVERKSETTER_VEDTAK,
        "iverksettelse",
        "Iverksett vedtak fra en behandling.  Forutsetter at et vedtak er fattet",
    ),
    AVSLUTTET(10, false, false, Behandlingsstatus.AVSLUTTET, "avsluttet", "Behandlingen er ferdig behandlet"),
    ;

    companion object {
        fun finnNesteBehandlingssteg(
            behandlingssteg: Behandlingssteg,
            harVerge: Boolean,
            harManuelleBrevmottakere: Boolean,
        ): Behandlingssteg {
            val nesteBehandlingssteg =
                Behandlingssteg.Companion.fraSekvens(
                    behandlingssteg.sekvens + 1,
                    harManuelleBrevmottakere,
                )
            if (nesteBehandlingssteg == Behandlingssteg.VERGE && !harVerge) {
                // Hvis behandling opprettes ikke med verge, kan behandlingen flyttes til neste steg
                return Behandlingssteg.Companion.fraSekvens(
                    nesteBehandlingssteg.sekvens + 1,
                )
            }
            return nesteBehandlingssteg
        }

        fun fraSekvens(
            sekvens: Int,
            brevmottakerErstatterVerge: Boolean = false,
        ): Behandlingssteg {
            for (behandlingssteg in values()) {
                if (sekvens == behandlingssteg.sekvens) {
                    return when (behandlingssteg) {
                        Behandlingssteg.BREVMOTTAKER, Behandlingssteg.VERGE -> if (brevmottakerErstatterVerge) Behandlingssteg.BREVMOTTAKER else Behandlingssteg.VERGE
                        else -> behandlingssteg
                    }
                }
            }
            throw IllegalArgumentException("Behandlingssteg finnes ikke med sekvens=$sekvens")
        }

        fun fraNavn(navn: String): Behandlingssteg =
            values().firstOrNull { it.name == navn }
                ?: throw IllegalArgumentException("Ukjent Behandlingssteg $navn")
    }
}
