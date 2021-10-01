package no.nav.familie.tilbake.integration.familie

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Fil
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.tilbake.config.IntegrasjonerConfig

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class IntegrasjonerClient(@Qualifier("azure") restOperations: RestOperations,
                          private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "familie.integrasjoner") {


    override val pingUri: URI =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri).path(IntegrasjonerConfig.PATH_PING).build().toUri()

    private val arkiverUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_ARKIVER)
            .build()
            .toUri()

    private val distribuerUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_DISTRIBUER)
            .build()
            .toUri()

    private val sftpUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_SFTP)
            .build()
            .toUri()

    private fun hentSaksbehandlerUri(id: String) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
                    .pathSegment(IntegrasjonerConfig.PATH_SAKSBEHANDLER)
                    .pathSegment(id)
                    .build()
                    .toUri()

    private fun hentTilgangssjekkUri() =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
                    .pathSegment(IntegrasjonerConfig.PATH_TILGANGSSJEKK)
                    .build()
                    .toUri()

    private fun hentOrganisasjonUri(organisasjonsnummer: String) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
                    .pathSegment(IntegrasjonerConfig.PATH_ORGANISASJON)
                    .pathSegment(organisasjonsnummer)
                    .build()
                    .toUri()

    private fun validerOrganisasjonUri(organisasjonsnummer: String) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
                    .pathSegment(IntegrasjonerConfig.PATH_ORGANISASJON)
                    .pathSegment(organisasjonsnummer)
                    .pathSegment("valider")
                    .build()
                    .toUri()

    private fun hentJournalpostUri() =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
                    .pathSegment(IntegrasjonerConfig.PATH_JOURNALPOST)
                    .build()
                    .toUri()

    private fun hentJournalpostHentDokumentUri(journalpostId: String, dokumentInfoId: String) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
                    .pathSegment(IntegrasjonerConfig.PATH_HENTDOKUMENT)
                    .pathSegment(journalpostId)
                    .path(dokumentInfoId)
                    .build()
                    .toUri()

    private fun hentNavkontorUri(enhetsId: String) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
                    .pathSegment(IntegrasjonerConfig.PATH_NAVKONTOR)
                    .path(enhetsId)
                    .build()
                    .toUri()


    fun arkiver(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        val response =
                postForEntity<Ressurs<ArkiverDokumentResponse>>(arkiverUri, arkiverDokumentRequest)
        return response.getDataOrThrow()
    }

    fun sendFil(fil: Fil) {
        putForEntity<Any>(sftpUri, fil)
    }

    fun distribuerJournalpost(journalpostId: String,
                              fagsystem: Fagsystem): String {
        val request = DistribuerJournalpostRequest(journalpostId,
                                                   fagsystem,
                                                   integrasjonerConfig.applicationName)

        return postForEntity<Ressurs<String>>(distribuerUri, request).getDataOrThrow()
    }

    fun hentDokument(dokumentInfoId: String, journalpostId: String): ByteArray {
        return getForEntity<Ressurs<ByteArray>>(hentJournalpostHentDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
    }

    fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon {
        return getForEntity<Ressurs<Organisasjon>>(hentOrganisasjonUri(organisasjonsnummer)).getDataOrThrow()
    }

    fun validerOrganisasjon(organisasjonsnummer: String): Boolean {
        return try {
            getForEntity<Ressurs<Boolean>>(validerOrganisasjonUri(organisasjonsnummer)).data == true
        } catch (e: Exception) {
            log.error("Organisasjonsnummeret $organisasjonsnummer er ikke gyldig. Feiler med ${e.message}")
            false
        }
    }

    fun hentSaksbehandler(id: String): Saksbehandler {
        return getForEntity<Ressurs<Saksbehandler>>(hentSaksbehandlerUri(id)).getDataOrThrow()
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse {
        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString() + "${IntegrasjonerConfig.PATH_OPPGAVE}/opprett")

        return postForEntity<Ressurs<OppgaveResponse>>(uri,
                                                       opprettOppgave,
                                                       HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString()
                             + "${IntegrasjonerConfig.PATH_OPPGAVE}/${patchOppgave.id}/oppdater")

        return patchForEntity<Ressurs<OppgaveResponse>>(uri,
                                                        patchOppgave,
                                                        HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()
    }

    fun finnOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {

        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString()
                             + IntegrasjonerConfig.PATH_OPPGAVE + "/v4")

        return postForEntity<Ressurs<FinnOppgaveResponseDto>>(uri,
                                                              finnOppgaveRequest,
                                                              HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString()
                             + IntegrasjonerConfig.PATH_OPPGAVE + "/$oppgaveId/ferdigstill")

        patchForEntity<Ressurs<OppgaveResponse>>(uri, "", HttpHeaders().medContentTypeJsonUTF8())

    }

    fun hentNavkontor(enhetsId: String): NavKontorEnhet {
        return getForEntity<Ressurs<NavKontorEnhet>>(hentNavkontorUri(enhetsId)).getDataOrThrow()
    }


    /*
     * Sjekker personene i behandlingen er egen ansatt, kode 6 eller kode 7. Og om saksbehandler har rettigheter til å behandle
     * slike personer.
     */
    fun sjekkTilgangTilPersoner(personIdenter: List<String>): List<Tilgang> {
        return postForEntity(hentTilgangssjekkUri(), personIdenter)
    }

    fun hentJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        secureLogger.info("henter journalposter for bruker med ident ${journalposterForBrukerRequest.brukerId} " +
                          "og data $journalposterForBrukerRequest")

        return postForEntity<Ressurs<List<Journalpost>>>(hentJournalpostUri(), journalposterForBrukerRequest).getDataOrThrow()
    }

}

fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
    this.add("Content-Type", "application/json;charset=UTF-8")
    this.acceptCharset = listOf(Charsets.UTF_8)
    return this
}
