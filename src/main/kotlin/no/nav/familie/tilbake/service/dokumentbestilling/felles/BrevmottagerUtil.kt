package no.nav.familie.tilbake.service.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge as VergeDto
import no.nav.familie.tilbake.behandling.domain.Verge as DomainVerge

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

    fun getVergenavn(verge: DomainVerge?, adresseinfo: Adresseinfo): String {
        return if (Vergetype.ADVOKAT == verge?.type) {
            adresseinfo.annenMottagersNavn!! // Når verge er advokat, viser vi verge navn som "Virksomhet navn v/ verge navn"
        } else {
            verge?.navn ?: ""
        }
    }

    fun getVergenavn(verge: VergeDto?, adresseinfo: Adresseinfo): String {
        return if (Vergetype.ADVOKAT == verge?.vergetype) {
            adresseinfo.annenMottagersNavn!! // Når verge er advokat, viser vi verge navn som "Virksomhet navn v/ verge navn"
        } else {
            verge?.navn ?: ""
        }
    }

}
