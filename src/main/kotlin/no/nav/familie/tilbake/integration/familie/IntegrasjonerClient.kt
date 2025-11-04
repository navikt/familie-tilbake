package no.nav.familie.tilbake.integration.familie

import AbstractPingableRestClient
import no.nav.familie.tilbake.config.IntegrasjonerConfig
import no.nav.familie.tilbake.kontrakter.Fil
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.kontrakter.dokdist.ManuellAdresse
import no.nav.familie.tilbake.kontrakter.getDataOrThrow
import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost
import no.nav.familie.tilbake.kontrakter.journalpost.JournalposterForBrukerRequest
import no.nav.familie.tilbake.kontrakter.navkontor.NavKontorEnhet
import no.nav.familie.tilbake.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.familie.tilbake.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.familie.tilbake.kontrakter.oppgave.MappeDto
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgave
import no.nav.familie.tilbake.kontrakter.oppgave.OppgaveResponse
import no.nav.familie.tilbake.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon
import no.nav.familie.tilbake.kontrakter.saksbehandler.Saksbehandler
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class IntegrasjonerClient(
    @Qualifier("azure") restOperations: RestOperations,
    private val integrasjonerConfig: IntegrasjonerConfig,
) : AbstractPingableRestClient(restOperations, "familie.integrasjoner") {
    private val arkiverUri: URI =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_ARKIVER)
            .build()
            .toUri()

    private val distribuerUri: URI =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_DISTRIBUER)
            .build()
            .toUri()

    private val sftpUri: URI =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_SFTP)
            .build()
            .toUri()

    private fun hentSaksbehandlerUri(id: String) =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_SAKSBEHANDLER, id)
            .build()
            .toUri()

    private val opprettOppgaveUri =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, "opprett")
            .build()
            .toUri()

    private fun patchOppgaveUri(oppgave: Oppgave) =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, oppgave.id!!.toString(), "oppdater")
            .build()
            .toUri()

    private fun tilordneOppgaveNyEnhetUri(
        oppgaveId: Long,
        nyEnhet: String,
        fjernMappeFraOppgave: Boolean,
        nullstillTilordnetRessurs: Boolean,
    ) = UriComponentsBuilder
        .fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, oppgaveId.toString(), "enhet", nyEnhet)
        .queryParam("fjernMappeFraOppgave", fjernMappeFraOppgave)
        .queryParam("nullstillTilordnetRessurs", nullstillTilordnetRessurs)
        .build()
        .toUri()

    private val finnoppgaverUri =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, "v4")
            .build()
            .toUri()

    private fun ferdigstillOppgaveUri(oppgaveId: Long) =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, oppgaveId.toString(), "ferdigstill")
            .build()
            .toUri()

    private fun hentOrganisasjonUri(organisasjonsnummer: String) =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_ORGANISASJON, organisasjonsnummer)
            .build()
            .toUri()

    private fun validerOrganisasjonUri(organisasjonsnummer: String) =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_ORGANISASJON, organisasjonsnummer, "valider")
            .build()
            .toUri()

    private fun hentJournalpostUri() =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_JOURNALPOST)
            .build()
            .toUri()

    private fun hentJournalpostHentDokumentUri(
        journalpostId: String,
        dokumentInfoId: String,
    ) = UriComponentsBuilder
        .fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_HENTDOKUMENT, journalpostId, dokumentInfoId)
        .build()
        .toUri()

    private fun hentNavkontorUri(enhetsId: String) =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_NAVKONTOR, enhetsId)
            .build()
            .toUri()

    private fun finnMapperUri(enhetNr: String): URI =
        UriComponentsBuilder
            .fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, "mappe", "finn", enhetNr)
            .build()
            .toUri()

    fun arkiver(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        val response = postForEntity<Ressurs<ArkiverDokumentResponse>>(arkiverUri, arkiverDokumentRequest)
        return response.getDataOrThrow()
    }

    fun sendFil(fil: Fil) {
        putForEntity<Any>(sftpUri, fil)
    }

    fun distribuerJournalpost(
        journalpostId: String,
        fagsystem: FagsystemDTO,
        distribusjonstype: Distribusjonstype,
        distribusjonstidspunkt: Distribusjonstidspunkt,
        manuellAdresse: ManuellAdresse? = null,
    ): String {
        val request =
            DistribuerJournalpostRequest(
                journalpostId,
                fagsystem,
                integrasjonerConfig.applicationName,
                distribusjonstype,
                distribusjonstidspunkt,
                manuellAdresse,
            )
        return postForEntity<Ressurs<String>>(distribuerUri, request).getDataOrThrow()
    }

    fun hentDokument(
        dokumentInfoId: String,
        journalpostId: String,
    ): ByteArray = getForEntity<Ressurs<ByteArray>>(hentJournalpostHentDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()

    fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon = getForEntity<Ressurs<Organisasjon>>(hentOrganisasjonUri(organisasjonsnummer)).getDataOrThrow()

    fun validerOrganisasjon(organisasjonsnummer: String): Boolean =
        try {
            getForEntity<Ressurs<Boolean>>(validerOrganisasjonUri(organisasjonsnummer)).data == true
        } catch (e: Exception) {
            log.error("Organisasjonsnummeret $organisasjonsnummer er ikke gyldig. Feiler med ${e.message}")
            false
        }

    fun hentSaksbehandler(id: String): Saksbehandler = getForEntity<Ressurs<Saksbehandler>>(hentSaksbehandlerUri(id)).getDataOrThrow()

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse = postForEntity<Ressurs<OppgaveResponse>>(opprettOppgaveUri, opprettOppgave).getDataOrThrow()

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        val uri = patchOppgaveUri(patchOppgave)
        return patchForEntity<Ressurs<OppgaveResponse>>(uri, patchOppgave).getDataOrThrow()
    }

    internal fun tilordneOppgaveNyEnhet(
        oppgaveId: Long,
        nyEnhet: String,
        fjernMappeFraOppgave: Boolean,
        nullstillTilordnetRessurs: Boolean,
    ): OppgaveResponse {
        val uri = tilordneOppgaveNyEnhetUri(oppgaveId, nyEnhet, fjernMappeFraOppgave, nullstillTilordnetRessurs)
        return patchForEntity<Ressurs<OppgaveResponse>>(uri, "", HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()
    }

    fun finnOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto = postForEntity<Ressurs<FinnOppgaveResponseDto>>(finnoppgaverUri, finnOppgaveRequest).getDataOrThrow()

    fun ferdigstillOppgave(oppgaveId: Long) {
        patchForEntity<Ressurs<OppgaveResponse>>(ferdigstillOppgaveUri(oppgaveId), "")
    }

    @Cacheable("mappeCache")
    fun finnMapper(enhet: String): List<MappeDto> {
        val respons = getForEntity<Ressurs<List<MappeDto>>>(finnMapperUri(enhet))
        return respons.getDataOrThrow()
    }

    fun hentNavkontor(enhetsId: String): NavKontorEnhet = getForEntity<Ressurs<NavKontorEnhet>>(hentNavkontorUri(enhetsId)).getDataOrThrow()

    fun hentJournalposterForBruker(
        journalposterForBrukerRequest: JournalposterForBrukerRequest,
        logContext: SecureLog.Context,
    ): List<Journalpost> {
        SecureLog.medContext(logContext) {
            info(
                "henter journalposter for bruker med ident {} og data {}",
                journalposterForBrukerRequest.brukerId,
                journalposterForBrukerRequest.toString(),
            )
        }

        return postForEntity<Ressurs<List<Journalpost>>>(hentJournalpostUri(), journalposterForBrukerRequest).getDataOrThrow()
    }
}

fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
    this.add("Content-Type", "application/json;charset=UTF-8")
    this.acceptCharset = listOf(Charsets.UTF_8)
    return this
}
