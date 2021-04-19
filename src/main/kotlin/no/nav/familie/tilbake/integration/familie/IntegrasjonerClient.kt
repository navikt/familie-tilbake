package no.nav.familie.tilbake.integration.familie

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.config.IntegrasjonerConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class IntegrasjonerClient(@Qualifier("azure") restOperations: RestOperations,
                          private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "familie.integrasjoner") {


    override val pingUri: URI =
            UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri).path(IntegrasjonerConfig.PATH_PING).build().toUri()

    private val organisasjonUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_ORGANISASJON)
            .build()
            .toUri()

    private val arkiverUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_ARKIVER)
            .build()
            .toUri()

    private val distribuerUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_DISTRIBUER)
            .build()
            .toUri()

    private fun hentOrganisasjonUri(organisasjonsnummer: String) =
            UriComponentsBuilder.fromUri(organisasjonUri).pathSegment(organisasjonsnummer).build().toUri()


    fun arkiver(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        val response =
                postForEntity<Ressurs<ArkiverDokumentResponse>>(arkiverUri, arkiverDokumentRequest)
        return response.getDataOrThrow()
    }

    fun distribuerJournalpost(journalpostId: String, fagsystem: Fagsystem): String {
        val request = DistribuerJournalpostRequest(journalpostId,
                                                   fagsystem.name,
                                                   integrasjonerConfig.applicationName)

        return postForEntity<Ressurs<String>>(distribuerUri, request).getDataOrThrow()
    }

    fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon {
        return getForEntity<Ressurs<Organisasjon>>(hentOrganisasjonUri(organisasjonsnummer)).getDataOrThrow()
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): String {
        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString() + "/oppgave/opprett")

        return Result.runCatching {
            postForEntity<Ressurs<OppgaveResponse>>(uri, opprettOppgave, HttpHeaders().medContentTypeJsonUTF8())
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)

                    it.data?.oppgaveId?.toString() ?: throw IntegrasjonException("Response fra oppgave mangler oppgaveId.",
                                                                                      null,
                                                                                      uri,
                                                                                      opprettOppgave.ident?.ident)
                },
                onFailure = {
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                    throw IntegrasjonException("Kall mot integrasjon feilet ved opprett oppgave. response=$message",
                                               it,
                                               uri,
                                               opprettOppgave.ident?.ident)
                }
        )
    }
    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString() + "/oppgave/${patchOppgave.id}/oppdater")

        return Result.runCatching {
            patchForEntity<Ressurs<OppgaveResponse>>(uri, patchOppgave, HttpHeaders().medContentTypeJsonUTF8())
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    it.getDataOrThrow()
                },
                onFailure = {
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                    throw IntegrasjonException("Kall mot integrasjon feilet ved oppdater oppgave. response=$message",
                                               it,
                                               uri,
                                               patchOppgave.identer?.find { ident ->
                                                   ident.gruppe == IdentGruppe.FOLKEREGISTERIDENT
                                               }?.ident)
                }
        )
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String?): String {
        val baseUri = URI.create(integrasjonerConfig.integrasjonUri.toString() + "/oppgave/$oppgaveId/fordel")
        val uri = if (saksbehandler == null)
            baseUri
        else
            UriComponentsBuilder.fromUri(baseUri).queryParam("saksbehandler", saksbehandler).build().toUri()

        return Result.runCatching {
            postForEntity<Ressurs<OppgaveResponse>>(uri, HttpHeaders().medContentTypeJsonUTF8())
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)

                    it.data?.oppgaveId?.toString() ?: throw IntegrasjonException("Response fra oppgave mangler oppgaveId.",
                                                                                 null,
                                                                                 uri
                    )
                },
                onFailure = {
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                    throw IntegrasjonException("Kall mot integrasjon feilet ved fordel oppgave. response=$message",
                                               it,
                                               uri
                    )
                }
        )
    }

    fun finnOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        return finnOppgaveRequest.run {
            val uri = URI.create(integrasjonerConfig.integrasjonUri.toString() + "/oppgave/v4")

            try {
                val ressurs =
                        postForEntity<Ressurs<FinnOppgaveResponseDto>>(uri,
                                                                       finnOppgaveRequest,
                                                                       HttpHeaders().medContentTypeJsonUTF8())
                assertGenerelleSuksessKriterier(ressurs)
                ressurs.data ?: throw IntegrasjonException("Ressurs mangler.", null, uri, null)
            } catch (e: Exception) {
                val message = if (e is RestClientResponseException) e.responseBodyAsString else ""
                throw IntegrasjonException("Kall mot integrasjon feilet ved hentOppgaver. response=$message",
                                           e,
                                           uri,
                                           "behandlingstema: $behandlingstema, oppgavetype: $oppgavetype, enhet: $enhet, saksbehandler: $saksbehandler")
            }
        }
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create(integrasjonerConfig.integrasjonUri.toString() + "/oppgave/$oppgaveId/ferdigstill")

        Result.runCatching {
            val response = patchForEntity<Ressurs<OppgaveResponse>>(uri, "")
            assertGenerelleSuksessKriterier(response)
        }.onFailure {
            throw IntegrasjonException("Kan ikke ferdigstille $oppgaveId. response=${it.message}", it, uri)
        }
    }
}

fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
    this.add("Content-Type", "application/json;charset=UTF-8")
    this.acceptCharset = listOf(Charsets.UTF_8)
    return this
}

inline fun <reified T> assertGenerelleSuksessKriterier(it: Ressurs<T>?) {
    val status = it?.status ?: error("Finner ikke ressurs")
    if (status == Ressurs.Status.SUKSESS && it.data == null) error("Ressurs har status suksess, men mangler data")
}