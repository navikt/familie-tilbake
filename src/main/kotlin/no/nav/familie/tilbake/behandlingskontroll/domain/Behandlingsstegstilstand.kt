package no.nav.familie.tilbake.behandlingskontroll.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Behandlingsstegstilstand(@Id
                                    val id: UUID = UUID.randomUUID(),
                                    val behandlingId: UUID,
                                    val behandlingssteg: Behandlingssteg,
                                    val behandlingsstegsstatus: Behandlingsstegstatus,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                    val sporbar: Sporbar = Sporbar())

enum class Behandlingssteg(val sekvens: Int, val kanSaksbehandles: Boolean, private val beskrivelse: String) {

    VARSEL(1, false, "Vurdere om varsel om tilbakekreving skal sendes til søker"),
    GRUNNLAG(2, false, "Mottat kravgrunnlag fra økonomi for tilbakekrevingsrevurdering"),
    VERGE(3, true, "Fakta om verge"),
    FAKTA(4, true, "Fakta om Feilutbetaling"),
    FORELDELSE(5, true, "Vurder om feilutbetalte perioder er foreldet"),
    VILKÅRSVURDERING(6, true, "Vurdere om og hva som skal tilbakekreves"),
    FORESLÅ_VEDTAK(7, true, "Foreslår vedtak"),
    FATTE_VEDTAK(8, true, "Fatter vedtak"),
    IVERKSETT_VEDTAK(9, false, "Iverksett vedtak fra en behandling.  Forutsetter at et vedtak er fattet"),
    AVSLUTTET(10, false, "Behandlingen er ferdig behandlet");

    companion object {

        fun finnNesteBehandlingssteg(behandlingssteg: Behandlingssteg): Behandlingssteg {
            val behandlingsnesteSteg = fraSekvens(behandlingssteg.sekvens + 1)
            if (behandlingsnesteSteg == VERGE) {
                return fraSekvens(behandlingsnesteSteg.sekvens + 1) // VERGE kan ikke opprettes automatisk, hopper til neste steg.
            }
            return behandlingsnesteSteg
        }

        private fun fraSekvens(sekvens: Int): Behandlingssteg {
            for (behandlingssteg in values()) {
                if (sekvens == behandlingssteg.sekvens) {
                    return behandlingssteg
                }
            }
            throw IllegalArgumentException("Behandlingssteg finnes ikke med sekvens=$sekvens")
        }
    }


}

enum class Behandlingsstegstatus(private val beskrivelse: String) {
    STARTET("Startet å utføre steg, en midlertidig status"),
    VENTER("Steget er satt på vent, f.eks. venter på brukertilbakemelding eller kravgrunnlag"),
    KLAR("Klar til saksbehandling"),
    UTFØRT("Steget er ferdig utført"),
    AUTOUTFØRT("Steget utføres automatisk av systemet"),
    TILBAKEFØRT("Steget er avbrutt og tilbakeført til et tidligere steg"),
    AVBRUTT("Steget er avbrutt");

    companion object {

        val aktiveStegStatuser = listOf(VENTER, KLAR)
        val utførteStegStatuser = listOf(UTFØRT, AUTOUTFØRT)

        fun erStegAktiv(status: Behandlingsstegstatus): Boolean {
            return aktiveStegStatuser.contains(status)
        }

        fun erStegUtført(status: Behandlingsstegstatus): Boolean {
            return utførteStegStatuser.contains(status)
        }
    }
}
