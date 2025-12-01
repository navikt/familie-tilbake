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
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil.Companion.TITTEL_VARSEL_TILBAKEBETALING
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.log.callId
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.ArkivDokument
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.ArkivDokumentvariant
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Behandlingstema
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.BrukerIdType
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.DokarkivBruker
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Dokumentkategori
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.JournalpostType
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Sak
import no.nav.tilbakekreving.pdf.Dokumentvariant
import no.nav.tilbakekreving.pdf.PdfGenerator
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header.TekstformatererHeader
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Brevdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokprodTilHtml
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Profile
import java.util.UUID

@Profile("dev", "prod")
class DokarkivClientImpl(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val varselbrevUtil: VarselbrevUtil,
    private val pdfFactory: () -> PdfGenerator = { PdfGenerator() },
    private val httpClient: HttpClient = HttpClient(CIO) { install(ContentNegotiation) { jackson() } },
) : DokarkivClient {
    private val logger = TracedLogger.getLogger<DokarkivClientImpl>()

    override suspend fun lagJournalpost(
        request: OpprettJournalpostRequest,
        ferdigstill: Boolean,
        behandlingId: String,
        eksternFagsakId: String,
        logContext: SecureLog.Context,
    ): OpprettJournalpostResponse {
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

    override fun journalførVarselbrev(
        varselbrevBehov: VarselbrevBehov,
        logContext: SecureLog.Context,
    ): OpprettJournalpostResponse {
        val brevdata = hentBrevdata(varselbrevBehov, logContext)

        val request = OpprettJournalpostRequest(
            journalpostType = JournalpostType.UTGAAENDE,
            behandlingstema = Behandlingstema.Tilbakebetaling.value,
            kanal = null,
            tittel = brevdata.tittel,
            tema = varselbrevBehov.ytelse.tilTema().name,
            avsenderMottaker = AvsenderMottaker(
                id = varselbrevBehov.brukerinfo.ident,
                idType = AvsenderMottakerIdType.FNR,
                navn = varselbrevBehov.brukerinfo.navn,
            ),
            bruker = DokarkivBruker(
                BrukerIdType.FNR,
                varselbrevBehov.brukerinfo.ident,
            ),
            dokumenter = hentArkivDokumenter(varselbrevBehov, brevdata, logContext),
            eksternReferanseId = lagEksternReferanseId(
                varselbrevBehov.behandlingId,
                Brevtype.VARSEL,
                brevdata.mottager,
            ),
            journalfoerendeEnhet = requireNotNull(varselbrevBehov.behandlendeEnhet) { "Enhetskode kreves for journalføring" }.kode,
            sak = Sak(
                arkivsaksnummer = null,
                arkivsaksystem = null,
                fagsakId = varselbrevBehov.eksternFagsakId,
                fagsaksystem = varselbrevBehov.ytelse.tilDokarkivFagsaksystem(),
                sakstype = "FAGSAK",
            ),
        )

        return runBlocking {
            lagJournalpost(
                ferdigstill = true,
                request = request,
                behandlingId = varselbrevBehov.behandlingId.toString(),
                eksternFagsakId = varselbrevBehov.eksternFagsakId,
                logContext = logContext,
            )
        }
    }

    private fun hentBrevdata(
        varselbrevBehov: VarselbrevBehov,
        logContext: SecureLog.Context,
    ): Brevdata {
        val brevmetadata = hentBrevMetadata(varselbrevBehov)
        val varselbrevsdokument = Varselbrevsdokument(
            brevmetadata = brevmetadata,
            beløp = varselbrevBehov.feilutbetaltBeløp,
            revurderingsvedtaksdato = varselbrevBehov.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = varselbrevBehov.varselbrev.fristForTilbakemelding,
            varseltekstFraSaksbehandler = varselbrevBehov.varseltekstFraSaksbehandler,
            feilutbetaltePerioder = varselbrevBehov.feilutbetaltePerioder,
        )

        return Brevdata(
            mottager = Brevmottager.BRUKER,
            metadata = brevmetadata,
            tittel = brevmetadata.tittel,
            overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevmetadata, false),
            brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false),
            vedleggHtml = hentVedlegg(varselbrevsdokument, varselbrevBehov.eksternFagsakId, logContext),
        )
    }

    private fun hentBrevMetadata(varselbrevBehov: VarselbrevBehov): Brevmetadata {
        return Brevmetadata(
            sakspartId = varselbrevBehov.brukerinfo.ident,
            sakspartsnavn = varselbrevBehov.brukerinfo.navn,
            tittel = TITTEL_VARSEL_TILBAKEBETALING + varselbrevBehov.ytelse.tilYtelseDTO(),
            mottageradresse = Adresseinfo(varselbrevBehov.brukerinfo.ident, varselbrevBehov.brukerinfo.navn),
            behandlendeEnhetsNavn = requireNotNull(varselbrevBehov.behandlendeEnhet) { "Enhetsnavn kreves for journalføring" }.navn,
            ansvarligSaksbehandler = requireNotNull(varselbrevBehov.varselbrev.ansvarligSaksbehandlerIdent) { "ansvarligSaksbehandlerIdent kreves for journalføring" },
            saksnummer = varselbrevBehov.eksternFagsakId,
            språkkode = varselbrevBehov.brukerinfo.språkkode,
            ytelsestype = varselbrevBehov.ytelse.tilYtelseDTO(),
            gjelderDødsfall = varselbrevBehov.gjelderDødsfall,
            institusjon = null, // Todo når vi skal håndtere institusjon
        )
    }

    private fun hentVedlegg(
        varselbrevsdokument: Varselbrevsdokument,
        eksternFagsakId: String,
        logContext: SecureLog.Context,
    ): String {
        return varselbrevUtil.lagVedlegg(
            varselbrevsdokument,
            eksternFagsakId,
            varselbrevsdokument.beløp,
            logContext,
        )
    }

    private fun hentArkivDokumenter(varselbrevBehov: VarselbrevBehov, brevdata: Brevdata, logContext: SecureLog.Context): List<ArkivDokument> {
        return listOf(
            ArkivDokument(
                brevkode = varselbrevBehov.ytelse.tilFagsystemDTO().name + "-TILB",
                dokumentKategori = Dokumentkategori.B,
                tittel = brevdata.tittel,
                dokumentvarianter = listOf(
                    ArkivDokumentvariant(
                        filtype = Filtype.PDFA.name,
                        variantformat = "ARKIV",
                        fysiskDokument = hentPdf(brevdata, logContext),
                        filnavn = "varselbrev.pdf",
                    ),
                ),
            ),
        )
    }

    private fun hentPdf(brevdata: Brevdata, logContext: SecureLog.Context): ByteArray {
        val pdfGenerator = pdfFactory()
        val html = lagHtml(brevdata)
        try {
            return pdfGenerator.genererPDFMedLogo(
                html = html,
                dokumentvariant = Dokumentvariant.ENDELIG,
                dokumenttittel = brevdata.overskrift,
            )
        } catch (e: Exception) {
            logger.medContext(logContext) {
                info("Feil ved generering av brev: brevData=$brevdata, html=$html", e)
            }
            throw e
        }
    }

    private fun lagHtml(data: Brevdata): String {
        val header = TekstformatererHeader.lagHeader(
            brevmetadata = data.metadata,
            overskrift = data.overskrift,
        )
        val innholdHtml = DokprodTilHtml.dokprodInnholdTilHtml(data.brevtekst)
        return header + innholdHtml + data.vedleggHtml
    }

    private fun lagEksternReferanseId(
        behandlingId: UUID,
        brevtype: Brevtype,
        mottager: Brevmottager,
    ): String {
        // alle brev kan potensielt bli sendt til både bruker og kopi verge. 2 av breva kan potensielt bli sendt flere gonger
        val callId = callId()
        return "${behandlingId}_${brevtype.name.lowercase()}_${mottager.name.lowercase()}_$callId"
    }
}
