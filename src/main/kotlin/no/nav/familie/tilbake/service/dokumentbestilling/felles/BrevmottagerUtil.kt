package no.nav.familie.tilbake.service.dokumentbestilling.felles

import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandling.domain.Vergetype

object BrevmottagerUtil {

    fun getannenMottagersNavn(brevmetadata: Brevmetadata): String? {
        val mottagernavn: String = brevmetadata.mottageradresse.mottagernavn
        val brukernavn = brevmetadata.sakspartsnavn
        val vergenavn = brevmetadata.vergenavn

        return if (mottagernavn.equals(brukernavn, ignoreCase = true)) {
            if (brevmetadata.finnesVerge) vergenavn else ""
        } else {
            brukernavn
        }
    }

    fun getVergenavn(verge: Verge?, adresseinfo: Adresseinfo): String {
        return if (Vergetype.ADVOKAT == verge?.type) {
            adresseinfo.annenMottagersNavn!! // Når verge er advokat, viser vi verge navn som "Virksomhet navn v/ verge navn"
        } else {
            verge?.navn ?: ""
        }
    }
}
