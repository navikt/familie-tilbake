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
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil.Companion.TITTEL_VARSEL_TILBAKEBETALING
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.log.callId
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.ArkivDokument
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Behandlingstema
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.BrukerIdType
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.DokarkivBruker
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Dokumentkategori
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Dokumenttype
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Dokumentvariant
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Fagsaksystem
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.JournalpostType
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Sak
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
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
import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID

@Profile("dev", "prod")
@Service
class DokarkivServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val varselbrevUtil: VarselbrevUtil,
) : DokarkivService {
    private val logger = TracedLogger.getLogger<DokarkivServiceImpl>()

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    override suspend fun lagJournalpost(
        tilbakekreving: Tilbakekreving,
        request: OpprettJournalpostRequest,
        ferdigstill: Boolean,
    ): OpprettJournalpostResponse {
        val logContext = SecureLog.Context.fra(tilbakekreving)

        val token = tokenValidationContextHolder.getTokenValidationContext().firstValidToken ?: error("Trenger token!")

        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.internId
        val fagsystemId = tilbakekreving.fagsystemId
        val baseUrl = applicationProperties.dokarkiv.baseUrl
        val scope = applicationProperties.dokarkiv.scope

        val callContext = CallContext.Saksbehandler(
            behandlingId.toString(),
            fagsystemId,
            userToken = token.encodedToken,
        )

        val bearerToken = tokenExchangeService.onBehalfOfToken(callContext.userToken, scope)

        try {
            val response = client.post("$baseUrl/rest/journalpostapi/v1/journalpost") {
                url { parameters.append("forsoekFerdigstill", ferdigstill.toString()) }
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $bearerToken")
                setBody(request)
            }

            if (response.status.isSuccess()) {
                return response.body<OpprettJournalpostResponse>()
            } else {
                val body = response.bodyAsText()

                logger.medContext(logContext) {
                    error("arkivering feilet: ${response.status}: $body")
                }
                return OpprettJournalpostResponse()
            }
        } catch (e: Exception) {
            logger.medContext(logContext) {
                error("arkivering feilet: ${e.message}")
            }
            throw Feil(
                message = "Utsending av brev feilet: ${e.message}",
                frontendFeilmelding = "Utsending av brev feilet.",
                logContext = SecureLog.Context.fra(tilbakekreving),
            )
        }
    }

// ============================================================================
// Alt under her er kunn for testing i dev. Disse har vi allerede i journalføringService og må fjernes her ifra.

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

    override fun journalføringTest(tilbakekreving: Tilbakekreving): OpprettJournalpostResponse {
        val logContext = SecureLog.Context.fra(tilbakekreving)
        val pdfGenerator = PdfGenerator()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        val bruker = tilbakekreving.bruker!!
        val fagsak = tilbakekreving.eksternFagsak

        val brevmetadata = Brevmetadata(
            sakspartId = bruker.tilFrontendDto().personIdent,
            sakspartsnavn = bruker.tilFrontendDto().navn,
            mottageradresse = Adresseinfo(bruker.tilFrontendDto().personIdent, bruker.tilFrontendDto().navn),
            behandlendeEnhetsNavn = behandling.hentBehandlingsinformasjon().enhet!!.navn,
            ansvarligSaksbehandler = behandling.hentBehandlingsinformasjon().ansvarligSaksbehandler.ident,
            saksnummer = fagsak.eksternId,
            språkkode = bruker.språkkode ?: Språkkode.NB,
            ytelsestype = fagsak.tilFrontendDto().ytelsestype,
            gjelderDødsfall = bruker.tilFrontendDto().dødsdato != null,
            institusjon = null, // Todo
        )

        val varselbrevsdokument = Varselbrevsdokument(
            brevmetadata = brevmetadata.copy(tittel = TITTEL_VARSEL_TILBAKEBETALING + fagsak.tilFrontendDto().ytelsestype),
            beløp = behandling.hentBehandlingsinformasjon().totaltFeilutbetaltBeløp.toLong(),
            revurderingsvedtaksdato = fagsak.behandlinger.nåværende().entry.vedtaksdato,
            fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
            varseltekstFraSaksbehandler = "Todo ", // todo Kanskje vi skal ha en varselTekst i behandling?
            feilutbetaltePerioder = behandling.hentBehandlingsinformasjon().feilutbetaltePerioder,
        )

        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, false)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)

        val vedlegg =
            varselbrevUtil.lagVedlegg(
                varselbrevsdokument,
                fagsak.eksternId,
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

        tilbakekreving.eksternFagsak.tilFrontendDto().fagsystem

        val avsenderMottaker = AvsenderMottaker(
            id = tilbakekreving.bruker!!.tilFrontendDto().personIdent,
            idType = AvsenderMottakerIdType.FNR,
            navn = tilbakekreving.bruker!!.tilFrontendDto().navn,
        )
        val dokarkivBruker = DokarkivBruker(
            BrukerIdType.FNR,
            tilbakekreving.bruker!!.tilFrontendDto().personIdent,
        )

        val dokumenter = listOf(
            ArkivDokument(
                brevkode = tilbakekreving.eksternFagsak.tilFrontendDto().fagsystem.name + "-TILB", // metadata.brevkode,
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
            tema = mapTilTema(tilbakekreving.eksternFagsak.tilFrontendDto().fagsystem).name, // dokumentMetadata.tema.name,
            avsenderMottaker = avsenderMottaker,
            bruker = dokarkivBruker,
            dokumenter = dokumenter,
            eksternReferanseId = lagEksternReferanseId(
                tilbakekreving.behandlingHistorikk.nåværende().entry.internId,
                Brevtype.VARSEL,
                data.mottager,
            ),
            journalfoerendeEnhet = behandling.hentBehandlingsinformasjon().enhet!!.kode,
            sak = Sak(
                tilbakekreving.eksternFagsak.eksternId,
                sakstype = "FAGSAK",
                fagsaksystem = when (fagsak.tilFrontendDto().fagsystem) {
                    FagsystemDTO.BA -> Fagsaksystem.BA
                    FagsystemDTO.EF -> Fagsaksystem.EF
                    FagsystemDTO.TS -> Fagsaksystem.TILLEGGSSTONADER
                    FagsystemDTO.IT01 -> Fagsaksystem.IT01
                    FagsystemDTO.KONT -> Fagsaksystem.KONT
                },
            ),
        )

        return runBlocking {
            lagJournalpost(
                tilbakekreving = tilbakekreving,
                ferdigstill = true,
                request = request,
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

    private fun mapTilTema(fagsystem: FagsystemDTO): Tema =
        when (fagsystem) {
            FagsystemDTO.EF -> Tema.ENF
            FagsystemDTO.KONT -> Tema.KON
            FagsystemDTO.BA -> Tema.BAR
            FagsystemDTO.TS -> Tema.TSO
            else -> error("Ugyldig fagsystem=${fagsystem.name}")
        }
}
