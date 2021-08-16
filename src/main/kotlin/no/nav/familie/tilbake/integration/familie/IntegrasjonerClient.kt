package no.nav.familie.tilbake.integration.familie

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Fil
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.DokumentInfo
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.familie.tilbake.config.IntegrasjonerConfig
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

interface IntegrasjonerClient {

    fun arkiver(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse
    fun sendFil(fil: Fil)
    fun distribuerJournalpost(journalpostId: String, fagsystem: Fagsystem): String
    fun hentDokument(dokumentInfoId: String, journalpostId: String): ByteArray
    fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon
    fun hentSaksbehandler(id: String): Saksbehandler
    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse
    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse
    fun fordelOppgave(oppgaveId: Long, saksbehandler: String?): OppgaveResponse
    fun finnOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto
    fun ferdigstillOppgave(oppgaveId: Long)
    fun hentNavkontor(enhetsId: String): NavKontorEnhet
}

@Component
@Profile("!e2e & !mock-integrasjoner")
class DefaultIntegrasjonerClient(@Qualifier("azure") restOperations: RestOperations,
                                 private val integrasjonerConfig: IntegrasjonerConfig)
    : IntegrasjonerClient, AbstractPingableRestClient(restOperations, "familie.integrasjoner") {


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

    private fun hentOrganisasjonUri(organisasjonsnummer: String) =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
                    .pathSegment(IntegrasjonerConfig.PATH_ORGANISASJON)
                    .pathSegment(organisasjonsnummer)
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


    override fun arkiver(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        val response =
                postForEntity<Ressurs<ArkiverDokumentResponse>>(arkiverUri, arkiverDokumentRequest)
        return response.getDataOrThrow()
    }

    override fun sendFil(fil: Fil) {
        putForEntity<Any>(sftpUri, fil)
    }

    override fun distribuerJournalpost(journalpostId: String,
                                       fagsystem: Fagsystem): String {
        val request = DistribuerJournalpostRequest(journalpostId,
                                                   fagsystem,
                                                   integrasjonerConfig.applicationName)

        return postForEntity<Ressurs<String>>(distribuerUri, request).getDataOrThrow()
    }

    override fun hentDokument(dokumentInfoId: String, journalpostId: String): ByteArray {
        return getForEntity<Ressurs<ByteArray>>(hentJournalpostHentDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
    }

    override fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon {
        return getForEntity<Ressurs<Organisasjon>>(hentOrganisasjonUri(organisasjonsnummer)).getDataOrThrow()
    }

    override fun hentSaksbehandler(id: String): Saksbehandler {
        return getForEntity<Ressurs<Saksbehandler>>(hentSaksbehandlerUri(id)).getDataOrThrow()
    }

    override fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse {
        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString() + "${IntegrasjonerConfig.PATH_OPPGAVE}/opprett")

        return postForEntity<Ressurs<OppgaveResponse>>(uri,
                                                       opprettOppgave,
                                                       HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()
    }

    override fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString()
                             + "${IntegrasjonerConfig.PATH_OPPGAVE}/${patchOppgave.id}/oppdater")

        return patchForEntity<Ressurs<OppgaveResponse>>(uri,
                                                        patchOppgave,
                                                        HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()
    }

    override fun fordelOppgave(oppgaveId: Long, saksbehandler: String?): OppgaveResponse {
        val baseUri = URI.create(integrasjonerConfig.integrasjonUri.toString()
                                 + "${IntegrasjonerConfig.PATH_OPPGAVE}/$oppgaveId/fordel")
        val uri = if (saksbehandler == null)
            baseUri
        else
            UriComponentsBuilder.fromUri(baseUri).queryParam("saksbehandler", saksbehandler).build().toUri()

        return postForEntity<Ressurs<OppgaveResponse>>(uri, HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()

    }

    override fun finnOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {

        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString()
                             + IntegrasjonerConfig.PATH_OPPGAVE + "/v4")

        return postForEntity<Ressurs<FinnOppgaveResponseDto>>(uri,
                                                              finnOppgaveRequest,
                                                              HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()
    }

    override fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString()
                             + IntegrasjonerConfig.PATH_OPPGAVE + "/$oppgaveId/ferdigstill")

        patchForEntity<Ressurs<OppgaveResponse>>(uri, "", HttpHeaders().medContentTypeJsonUTF8())

    }

    override fun hentNavkontor(enhetsId: String): NavKontorEnhet {
        return getForEntity<Ressurs<NavKontorEnhet>>(hentNavkontorUri(enhetsId)).getDataOrThrow()
    }
}

fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
    this.add("Content-Type", "application/json;charset=UTF-8")
    this.acceptCharset = listOf(Charsets.UTF_8)
    return this
}

@Component
@Profile("e2e")
class E2EIntegrasjonerClient : IntegrasjonerClient {

    override fun arkiver(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        logger.info("Skipper arkivering av dokument i e2e-profil")
        return ArkiverDokumentResponse("123", true, listOf(DokumentInfo("123")));
    }

    override fun sendFil(fil: Fil) {
        TODO("Not yet implemented")
    }

    override fun distribuerJournalpost(journalpostId: String, fagsystem: Fagsystem): String {
        logger.info("Skipper distribuering av journalpost i e2e-profil")
        return "abc12345"
    }

    override fun hentDokument(dokumentInfoId: String, journalpostId: String): ByteArray {
        logger.info("Skipper henting av dokument i e2e-profil")
        return ByteArray(10)
    }

    override fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon {
        logger.info("Skipper henting av organisasion for nummer ${organisasjonsnummer} i e2e-profil")
        return Organisasjon(organisasjonsnummer, "Dummy organisasjon");
    }

    override fun hentSaksbehandler(id: String): Saksbehandler {
        return Saksbehandler(UUID.randomUUID(), id, "Dummy", "Saksbehandler")
    }

    override fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse {
        logger.info("Skipper opprettelse av oppgave for behandling ${opprettOppgave.saksId} i e2e-profil")
        return OppgaveResponse(123456)
    }

    override fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        logger.info("Skipper patch oppgave i e2e-profil")
        return OppgaveResponse(123456)
    }

    override fun fordelOppgave(oppgaveId: Long, saksbehandler: String?): OppgaveResponse {
        TODO("Not yet implemented")
    }

    override fun finnOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        logger.info("Skipper finn oppgaver for behandling ${finnOppgaveRequest.saksreferanse} i e2e-profil")
        return FinnOppgaveResponseDto(0, listOf(Oppgave(id = 123456)))
    }

    override fun ferdigstillOppgave(oppgaveId: Long) {
        logger.info("Skipper ferdigstill oppgave i e2e-profil")
    }

    override fun hentNavkontor(enhetsId: String): NavKontorEnhet {
        TODO("Not yet implemented")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(E2EIntegrasjonerClient::class.java)
    }
}
