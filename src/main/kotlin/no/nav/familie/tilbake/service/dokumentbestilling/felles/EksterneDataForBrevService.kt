package no.nav.familie.tilbake.service.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge as VergeDto
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import org.springframework.stereotype.Service

@Service
class EksterneDataForBrevService(private val pdlClient: PdlClient) {

    fun hentPerson(ident: String, fagsystem: Fagsystem): Personinfo {
        return pdlClient.hentPersoninfo(ident, fagsystem)
    }

    fun hentAdresse(personinfo: Personinfo): Adresseinfo {
        return Adresseinfo(personinfo.ident, personinfo.navn)
    }

    fun hentAdresse(personinfo: Personinfo,
                    brevmottager: Brevmottager,
                    verge: Verge?,
                    fagsystem: Fagsystem): Adresseinfo {

        if (Vergetype.ADVOKAT == verge?.type) {
            return hentOrganisasjonsadresse(verge.orgNr!!, verge.navn, personinfo, brevmottager)
        } else if (Brevmottager.VERGE == brevmottager && verge != null) {
            val person = hentPerson(verge.ident!!, fagsystem)
            return hentAdresse(person)
        }
        return hentAdresse(personinfo)
    }

    fun hentAdresse(personinfo: Personinfo,
                    brevmottager: Brevmottager,
                    verge: VergeDto?,
                    fagsystem: Fagsystem): Adresseinfo {

        if (Vergetype.ADVOKAT == verge?.vergetype) {
            return hentOrganisasjonsadresse(verge.organisasjonsnummer!!, verge.navn, personinfo, brevmottager)
        } else if (Brevmottager.VERGE == brevmottager && verge != null) {
            val person = hentPerson(verge.personIdent!!, fagsystem)
            return hentAdresse(person)
        }
        return hentAdresse(personinfo)
    }

    private fun hentOrganisasjonsadresse(organisasjonsnummer: String,
                                         vergenavn: String,
                                         personinfo: Personinfo,
                                         brevmottager: Brevmottager): Adresseinfo {
//  TODO kommenteres inn når servicen i integrasjoner er oppe å kjøre
//        val virksomhet: Organisasjon = integrasjonerClient.hentOrganisasjon(organisasjonsnummer)
        val organisasjon = Organisasjon("987654321", "Dummy")
        return fra(organisasjon, vergenavn, personinfo, brevmottager)
    }

    private fun fra(organisasjon: Organisasjon,
                    vergeNavn: String,
                    personinfo: Personinfo,
                    brevmottager: Brevmottager): Adresseinfo {
        val organisasjonsnavn: String = organisasjon.navn
        val vedVergeNavn = "v/ $vergeNavn"
        val annenMottagersNavn = "$organisasjonsnavn $vedVergeNavn"
        return if (Brevmottager.VERGE == brevmottager) {
            Adresseinfo(personinfo.ident, organisasjonsnavn, personinfo.navn)
        } else {
            Adresseinfo(personinfo.ident, personinfo.navn, annenMottagersNavn)
        }
    }
}
