package no.nav.familie.tilbake.service.dokumentbestilling.felles

import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandling.domain.Vergetype

object BrevMottakerUtil {

    fun getAnnenMottakerNavn(brevMetadata: BrevMetadata): String? {
        val mottakerNavn: String = brevMetadata.mottakerAdresse.mottakerNavn
        val brukerNavn = brevMetadata.sakspartNavn
        val vergeNavn = brevMetadata.vergeNavn

        return if (mottakerNavn.equals(brukerNavn, ignoreCase = true)) {
            if (brevMetadata.finnesVerge) vergeNavn else ""
        } else {
            brukerNavn
        }
    }

    fun getVergeNavn(verge: Verge?, adresseinfo: Adresseinfo): String {
        return if (Vergetype.ADVOKAT == verge?.type) {
            adresseinfo.annenMottakerNavn!! // Når verge er advokat, viser vi verge navn som "Virksomhet navn v/ verge navn"
        } else {
            verge?.navn ?: ""
        }
    }
}
