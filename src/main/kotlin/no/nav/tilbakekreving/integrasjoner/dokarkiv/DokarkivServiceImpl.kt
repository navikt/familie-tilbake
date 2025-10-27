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
import no.nav.familie.tilbake.kontrakter.dokarkiv.Dokumenttype
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.log.callId
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.ArkivDokument
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Behandlingstema
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.BrukerIdType
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.DokarkivBruker
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Dokumentkategori
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Dokumentvariant
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Fagsaksystem
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.JournalpostType
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Sak
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
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
import org.springframework.stereotype.Service
import java.util.UUID

@Profile("dev", "prod")
@Service
class DokarkivServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val varselbrevUtil: VarselbrevUtil,
    private val pdfFactory: () -> PdfGenerator = { PdfGenerator() },
    private val httpClient: HttpClient = HttpClient(CIO) { install(ContentNegotiation) { jackson() } },
) : DokarkivService {
    private val logger = TracedLogger.getLogger<DokarkivServiceImpl>()

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
        val pdfGenerator = pdfFactory()
        val brevmetadata = Brevmetadata(
            sakspartId = varselbrevBehov.brukerIdent,
            sakspartsnavn = varselbrevBehov.brukerNavn,
            mottageradresse = Adresseinfo(varselbrevBehov.brukerIdent, varselbrevBehov.brukerNavn),
            behandlendeEnhetsNavn = "Oslo", // Todo for testing harkodet Oslo for nå. må endres til requireNotNull(varselbrevBehov.behandlendeEnhet) { "Enhetsnavn kreves for journalføring" }.navn,
            ansvarligSaksbehandler = "1234", // Todo for testing harkodet 1234 for nå. må endres til requireNotNull(varselbrevBehov.varselbrev.ansvarligSaksbehandlerIdent) {"ansvarligSaksbehandlerIdent kreves for journalføring"},
            saksnummer = varselbrevBehov.eksternFagsakId,
            språkkode = varselbrevBehov.språkkode,
            ytelsestype = varselbrevBehov.ytelse.tilYtelseDTO(),
            gjelderDødsfall = varselbrevBehov.gjelderDødsfall,
            institusjon = null, // Todo
        )

        val varselbrevsdokument = Varselbrevsdokument(
            brevmetadata = brevmetadata.copy(tittel = TITTEL_VARSEL_TILBAKEBETALING + varselbrevBehov.ytelse.tilYtelseDTO()),
            beløp = varselbrevBehov.feilutbetaltBeløp,
            revurderingsvedtaksdato = varselbrevBehov.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = varselbrevBehov.varselbrev.fristForTilbakemelding,
            varseltekstFraSaksbehandler = varselbrevBehov.varseltekstFraSaksbehandler,
            feilutbetaltePerioder = varselbrevBehov.feilutbetaltePerioder,
        )

        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, false)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)

        val vedlegg =
            varselbrevUtil.lagVedlegg(
                varselbrevsdokument,
                varselbrevBehov.eksternFagsakId,
                varselbrevsdokument.beløp,
                logContext,
            )

        val data = Brevdata(
            mottager = Brevmottager.BRUKER,
            metadata = varselbrevsdokument.brevmetadata,
            overskrift = overskrift,
            brevtekst = brevtekst,
            vedleggHtml = vedlegg,
        )

        val html = lagHtml(data)

        val pdf =
            try {
                pdfGenerator.genererPDFMedLogo(
                    html,
                    no.nav.tilbakekreving.pdf.Dokumentvariant.ENDELIG,
                    data.tittel ?: data.metadata.tittel ?: data.overskrift,
                )
            } catch (e: Exception) {
                logger.medContext(logContext) {
                    info("Feil ved generering av brev: brevData=$data, html=$html", e)
                }
                throw e
            }

        val dokument =
            Dokument(
                dokument = pdf,
                filtype = Filtype.PDFA,
                filnavn = "varselbrev.pdf",
                tittel = brevmetadata.tittel,
                dokumenttype = Dokumenttype.KONTANTSTØTTE_TILBAKEKREVING_BREV,
            )

        val avsenderMottaker = AvsenderMottaker(
            id = varselbrevBehov.brukerIdent,
            idType = AvsenderMottakerIdType.FNR,
            navn = varselbrevBehov.brukerNavn,
        )
        val dokarkivBruker = DokarkivBruker(
            BrukerIdType.FNR,
            varselbrevBehov.brukerIdent,
        )

        val dokumenter = listOf(
            ArkivDokument(
                brevkode = varselbrevBehov.ytelse.tilFagsystemDTO().name + "-TILB",
                dokumentKategori = Dokumentkategori.B,
                tittel = dokument.tittel,
                dokumentvarianter = listOf(
                    Dokumentvariant(
                        dokument.filtype.name,
                        variantformat = when (dokument.filtype) {
                            Filtype.PDFA -> "ARKIV" // ustrukturert dokumentDto
                        },
                        dokument.dokument,
                        dokument.filnavn,
                    ),
                ),
            ),
        )

        val request = OpprettJournalpostRequest(
            journalpostType = JournalpostType.UTGAAENDE,
            behandlingstema = Behandlingstema.Tilbakebetaling.value,
            kanal = null,
            tittel = dokument.tittel,
            tema = mapTilTema(varselbrevBehov.ytelse.tilFagsystemDTO()).name,
            avsenderMottaker = avsenderMottaker,
            bruker = dokarkivBruker,
            dokumenter = dokumenter,
            eksternReferanseId = lagEksternReferanseId(
                varselbrevBehov.behandlingId,
                Brevtype.VARSEL,
                data.mottager,
            ),
            journalfoerendeEnhet = "1234", // Todo for testing harkodet 1234 for nå. må fjernes!! requireNotNull(varselbrevBehov.behandlendeEnhet) { "Enhetskode kreves for journalføring" }.kode,,
            sak = Sak(
                arkivsaksnummer = null,
                arkivsaksystem = null,
                fagsakId = varselbrevBehov.eksternFagsakId,
                fagsaksystem = when (varselbrevBehov.ytelse.tilFagsystemDTO()) {
                    FagsystemDTO.BA -> Fagsaksystem.BA
                    FagsystemDTO.EF -> Fagsaksystem.EF
                    FagsystemDTO.TS -> Fagsaksystem.TILLEGGSSTONADER
                    FagsystemDTO.IT01 -> Fagsaksystem.IT01
                    FagsystemDTO.KONT -> Fagsaksystem.KONT
                    FagsystemDTO.AAP -> Fagsaksystem.KELVIN
                },
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

    private fun lagEksternReferanseId(
        behandlingId: UUID,
        brevtype: Brevtype,
        mottager: Brevmottager,
    ): String {
        // alle brev kan potensielt bli sendt til både bruker og kopi verge. 2 av breva kan potensielt bli sendt flere gonger
        val callId = callId()
        return "${behandlingId}_${brevtype.name.lowercase()}_${mottager.name.lowercase()}_$callId"
    }

    private fun lagHtml(data: Brevdata): String {
        val header = lagHeader(data)
        val innholdHtml = lagInnhold(data)
        return header + innholdHtml + data.vedleggHtml
    }

    private fun lagInnhold(data: Brevdata): String = DokprodTilHtml.dokprodInnholdTilHtml(data.brevtekst)

    private fun lagHeader(data: Brevdata): String =
        TekstformatererHeader.lagHeader(
            brevmetadata = data.metadata,
            overskrift = data.overskrift,
        )

    private fun mapTilTema(fagsystem: FagsystemDTO): Tema =
        when (fagsystem) {
            FagsystemDTO.EF -> Tema.ENF
            FagsystemDTO.KONT -> Tema.KON
            FagsystemDTO.BA -> Tema.BAR
            FagsystemDTO.TS -> Tema.TSO
            else -> error("Ugyldig fagsystem=${fagsystem.name}")
        }
}
