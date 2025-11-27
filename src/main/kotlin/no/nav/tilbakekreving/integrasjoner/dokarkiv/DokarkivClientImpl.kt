package no.nav.tilbakekreving.integrasjoner.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.ArkivDokument
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.ArkivDokumentvariant
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Behandlingstema
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.BrukerIdType
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.DokarkivBruker
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.JournalpostType
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Sak
import no.nav.tilbakekreving.kontrakter.ytelse.DokarkivFagsaksystem
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Dokumentklass
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Profile
import java.util.UUID

@Profile("dev", "prod")
class DokarkivClientImpl(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val httpClient: HttpClient = HttpClient(CIO) { install(ContentNegotiation) { jackson() } },
) : DokarkivClient {
    private val logger = TracedLogger.getLogger<DokarkivClientImpl>()

    private suspend fun lagJournalpost(
        request: OpprettJournalpostRequest,
        ferdigstill: Boolean,
        behandlingId: UUID,
    ): OpprettJournalpostResponse {
        val logContext = SecureLog.Context.tom()
        val baseUrl = applicationProperties.dokarkiv.baseUrl
        val scope = applicationProperties.dokarkiv.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        try {
            val response = httpClient.post("$baseUrl/rest/journalpostapi/v1/journalpost") {
                url { parameters.append("forsoekFerdigstill", ferdigstill.toString()) }
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (response.status.isSuccess()) {
                return response.body<OpprettJournalpostResponse>()
            } else {
                val body = response.bodyAsText()
                logger.medContext(logContext) {
                    error(
                        "Feil ved Journalføring av varselbrev for behandlingId=$behandlingId. " +
                            "Response status: ${response.status}. Response body: $body",
                    )
                }
                throw Feil(
                    message = "journalføring av brev feilet: $body",
                    frontendFeilmelding = "journalføring av brev feilet.",
                    logContext = logContext,
                )
            }
        } catch (e: Exception) {
            logger.medContext(logContext) {
                error("journalføring feilet: ${e.message}")
            }
            throw Feil(
                message = "journalføring av brev feilet: ${e.message}",
                frontendFeilmelding = "journalføring av brev feilet.",
                logContext = logContext,
            )
        }
    }

    override fun opprettOgSendJournalpostRequest(
        arkiverDokument: ArkiverDokumentRequest,
        fagsaksystem: DokarkivFagsaksystem,
        brevkode: String,
        tema: Tema,
        dokuemntkategori: Dokumentklass,
        behandlingId: UUID,
    ): OpprettJournalpostResponse {
        val dokument = arkiverDokument.hoveddokumentvarianter[0]

        val journalpostRequest = OpprettJournalpostRequest(
            journalpostType = JournalpostType.UTGAAENDE,
            behandlingstema = Behandlingstema.Tilbakebetaling.value,
            kanal = null,
            tittel = dokument.tittel,
            tema = tema.name,
            avsenderMottaker = arkiverDokument.avsenderMottaker!!,
            bruker = DokarkivBruker(BrukerIdType.FNR, arkiverDokument.fnr),
            dokumenter = listOf(
                ArkivDokument(
                    brevkode = brevkode,
                    dokumentKategori = dokuemntkategori,
                    tittel = dokument.tittel,
                    dokumentvarianter = listOf(
                        ArkivDokumentvariant(
                            filtype = dokument.filtype.name,
                            variantformat = "ARKIV",
                            fysiskDokument = dokument.dokument,
                            filnavn = dokument.filnavn,
                        ),
                    ),
                ),
            ),
            eksternReferanseId = arkiverDokument.eksternReferanseId!!,
            journalfoerendeEnhet = arkiverDokument.journalførendeEnhet!!,
            sak = Sak(
                arkivsaksnummer = null,
                arkivsaksystem = null,
                fagsakId = arkiverDokument.fagsakId,
                sakstype = "FAGSAK",
                fagsaksystem = fagsaksystem,
            ),
        )
        return runBlocking {
            lagJournalpost(
                ferdigstill = arkiverDokument.forsøkFerdigstill,
                request = journalpostRequest,
                behandlingId = behandlingId,
            )
        }
    }
}
