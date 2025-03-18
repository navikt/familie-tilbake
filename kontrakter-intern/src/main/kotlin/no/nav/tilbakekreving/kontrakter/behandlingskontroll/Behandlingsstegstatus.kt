package no.nav.tilbakekreving.kontrakter.behandlingskontroll

enum class Behandlingsstegstatus(
    private val beskrivelse: String,
) {
    VENTER("Steget er satt på vent, f.eks. venter på brukertilbakemelding eller kravgrunnlag"),
    KLAR("Klar til saksbehandling"),
    UTFØRT("Steget er ferdig utført"),
    AUTOUTFØRT("Steget utføres automatisk av systemet"),
    TILBAKEFØRT("Steget er avbrutt og tilbakeført til et tidligere steg"),
    AVBRUTT("Steget er avbrutt"),
    ;

    companion object {
        val aktiveStegStatuser =
            listOf(
                VENTER,
                KLAR,
            )
        private val utførteStegStatuser =
            listOf(
                UTFØRT,
                AUTOUTFØRT,
            )

        fun erStegAktiv(status: Behandlingsstegstatus): Boolean = Behandlingsstegstatus.Companion.aktiveStegStatuser.contains(status)

        fun erStegUtført(status: Behandlingsstegstatus): Boolean = Behandlingsstegstatus.Companion.utførteStegStatuser.contains(status)
    }
}
