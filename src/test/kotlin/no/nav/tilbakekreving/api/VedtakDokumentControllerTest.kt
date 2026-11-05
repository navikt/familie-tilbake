package no.nav.tilbakekreving.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.tilbakekreving.Testdata.dokumentInfo
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.kontrakter.frontend.models.DokumentInfoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.vedtak.VedtakDokumentRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class VedtakDokumentControllerTest {
    private val vedtakDokumentService = mockk<VedtakDokumentService>()
    private val vedtakDokumentRepository = mockk<VedtakDokumentRepository>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()
    private val tilgangskontrollService = mockk<TilgangskontrollService>()
    private val vedtaksdokumentTilgang = mockk<VedtaksdokumentTilgang>()
    private val controller = VedtakDokumentController(
        vedtakDokumentService = vedtakDokumentService,
        vedtakDokumentRepository = vedtakDokumentRepository,
        tilbakekrevingService = tilbakekrevingService,
        tilgangskontrollService = tilgangskontrollService,
        vedtaksdokumentTilgang = vedtaksdokumentTilgang,
    )
    private val vedtakId = KravgrunnlagGenerator.nextId(18).toBigInteger()
    private val tilbakekrevingId = KravgrunnlagGenerator.nextId(18)

    @BeforeEach
    fun nullstillMocker() {
        clearMocks(
            vedtakDokumentService,
            vedtakDokumentRepository,
            tilbakekrevingService,
            tilgangskontrollService,
            vedtaksdokumentTilgang,
        )
    }

    @Test
    fun `godkjent maskinkall uten saksbehandler med flere dokumenter og duplikater`() {
        every { vedtaksdokumentTilgang.validerMaskintilgang() } just Runs
        every { vedtakDokumentRepository.findTilbakekrevingIdByVedtakId(vedtakId) } returns tilbakekrevingId
        every { tilbakekrevingService.hentTilbakekreving(any(), true) } returns tilbakekrevingMedDokumenter(
            dokumentInfo("journalpost-1", "dokument-1", brevSendt = 1.januar(2025)),
            dokumentInfo("journalpost-1", "dokument-1", brevSendt = 2.januar(2025)),
            dokumentInfo("journalpost-1", "dokument-2", brevSendt = 1.januar(2025)),
            dokumentInfo("journalpost-2", "dokument-1", brevSendt = 1.januar(2025)),
        )

        val response = controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())

        response.statusCode shouldBe HttpStatus.OK
        response.body.shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(
            VedtaksdokumentDto(journalpostId = "journalpost-1", dokumentInfoId = "dokument-1"),
            VedtaksdokumentDto(journalpostId = "journalpost-1", dokumentInfoId = "dokument-2"),
            VedtaksdokumentDto(journalpostId = "journalpost-2", dokumentInfoId = "dokument-1"),
        )
        verifyOrder {
            vedtakDokumentRepository.findTilbakekrevingIdByVedtakId(vedtakId)
            vedtaksdokumentTilgang.validerMaskintilgang()
            tilbakekrevingService.hentTilbakekreving(any(), true)
        }
        verify(exactly = 0) { tilbakekrevingService.lesTilbakekreving(any(), any(), any()) }
        verify { listOf(vedtakDokumentService, tilgangskontrollService) wasNot Called }
    }

    @Test
    fun `avvist maskinkall`() {
        every { vedtakDokumentRepository.findTilbakekrevingIdByVedtakId(vedtakId) } returns tilbakekrevingId
        every { vedtaksdokumentTilgang.validerMaskintilgang() } throws ForbiddenError(
            message = "Ingen tilgang",
            frontendFeilmelding = "Ingen tilgang",
            logContext = SecureLog.Context.tom(),
        )

        shouldThrow<ForbiddenError> {
            controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())
        }

        verify(exactly = 1) { vedtaksdokumentTilgang.validerMaskintilgang() }
        verify {
            listOf(tilbakekrevingService, vedtakDokumentService, tilgangskontrollService) wasNot Called
        }
    }

    private fun tilbakekrevingMedDokumenter(vararg dokumenter: DokumentInfoDto): Tilbakekreving = mockk {
        every { brevHistorikk.alleSendteDokumenter() } returns dokumenter.toList()
    }
}
