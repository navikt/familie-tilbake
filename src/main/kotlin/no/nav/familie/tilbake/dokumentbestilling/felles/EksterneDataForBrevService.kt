package no.nav.familie.tilbake.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import org.springframework.stereotype.Service
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge as VergeDto

@Service
class EksterneDataForBrevService(private val pdlClient: PdlClient,
                                 private val integrasjonerClient: IntegrasjonerClient) {

    fun hentPerson(ident: String, fagsystem: Fagsystem): Personinfo {
        return pdlClient.hentPersoninfo(ident, fagsystem)
    }

    fun hentSaksbehandlernavn(id: String): String {
        val saksbehandler = integrasjonerClient.hentSaksbehandler(id)
        return saksbehandler.fornavn + " " + saksbehandler.etternavn
    }

    fun hentPåloggetSaksbehandlernavnMedDefault(defaultId: String): String {
        val saksbehandlerId = ContextService.hentPåloggetSaksbehandler(defaultId)
        val saksbehandler = integrasjonerClient.hentSaksbehandler(saksbehandlerId)
        return saksbehandler.fornavn + " " + saksbehandler.etternavn
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

        val organisasjon = integrasjonerClient.hentOrganisasjon(organisasjonsnummer)
        return lagAdresseinfo(organisasjon, vergenavn, personinfo, brevmottager)
    }

    private fun lagAdresseinfo(organisasjon: Organisasjon,
                               vergeNavn: String,
                               personinfo: Personinfo,
                               brevmottager: Brevmottager): Adresseinfo {
        val organisasjonsnavn: String = organisasjon.navn
        val vedVergeNavn = "v/ $vergeNavn"
        val annenMottagersNavn = "$organisasjonsnavn $vedVergeNavn"
        return if (Brevmottager.VERGE == brevmottager) {
            Adresseinfo(organisasjon.organisasjonsnummer, organisasjon.navn, personinfo.navn)
        } else {
            Adresseinfo(personinfo.ident, personinfo.navn, annenMottagersNavn)
        }
    }
}
