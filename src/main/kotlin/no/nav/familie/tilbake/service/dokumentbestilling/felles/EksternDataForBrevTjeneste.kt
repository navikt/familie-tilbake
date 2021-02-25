package no.nav.familie.tilbake.service.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandling.domain.Vergetype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.PersonInfo
import org.springframework.stereotype.Service

@Service
class EksternDataForBrevTjeneste(private val pdlClient: PdlClient,
                                 private val integrasjonerClient: IntegrasjonerClient) {


    fun hentPerson(ident: String, fagsystem: Fagsystem): PersonInfo {
        return pdlClient.hentPersoninfo(ident, fagsystem)
    }

    fun hentAdresse(personInfo: PersonInfo): Adresseinfo {
        return Adresseinfo(personInfo.ident, personInfo.navn)
    }

    fun hentAdresse(personInfo: PersonInfo,
                    brevMottaker: BrevMottaker,
                    verge: Verge?,
                    fagsystem: Fagsystem): Adresseinfo {

        if (Vergetype.ADVOKAT == verge?.type) {
            return hentOrganisasjonAdresse(verge.orgNr!!, verge.navn, personInfo, brevMottaker)
        } else if (BrevMottaker.VERGE == brevMottaker && verge != null) {
            val person = hentPerson(verge.ident!!, fagsystem)
            return hentAdresse(person)
        }
        return hentAdresse(personInfo)
    }

    private fun hentOrganisasjonAdresse(organisasjonNummer: String,
                                        vergeNavn: String,
                                        personInfo: PersonInfo,
                                        brevMottaker: BrevMottaker): Adresseinfo {
//  TODO kommenteres inn når tjenesten i integrasjoner er oppe å kjøre
//        val virksomhet: Organisasjon = integrasjonerClient.hentOrganisasjon(organisasjonNummer)
        val virksomhet = Organisasjon("987654321", "Dummy")
        return fra(virksomhet, vergeNavn, personInfo, brevMottaker)
    }

    private fun fra(virksomhet: Organisasjon,
                    vergeNavn: String,
                    personInfo: PersonInfo,
                    brevMottaker: BrevMottaker): Adresseinfo {
        val organisasjonNavn: String = virksomhet.navn
        val vedVergeNavn = "v/ $vergeNavn"
        val annenMottakerNavn = "$organisasjonNavn $vedVergeNavn"
        return if (BrevMottaker.VERGE == brevMottaker) {
            Adresseinfo(personInfo.ident, organisasjonNavn, personInfo.navn)
        } else {
            Adresseinfo(personInfo.ident, personInfo.navn, annenMottakerNavn)
        }
    }
}