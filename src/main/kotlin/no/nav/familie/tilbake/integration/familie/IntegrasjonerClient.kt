package no.nav.familie.tilbake.integration.familie

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.tilbake.config.IntegrasjonerConfig
import org.springframework.beans.factory.annotation.Qualifier
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
}
