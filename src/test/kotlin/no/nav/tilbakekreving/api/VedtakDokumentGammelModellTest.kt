package no.nav.tilbakekreving.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgangStub
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.math.BigInteger
import java.util.UUID

@ActiveProfiles("ny-modell")
class VedtakDokumentGammelModellTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var vedtakDokumentController: VedtakDokumentController

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @BeforeEach
    fun medToken() = medAuthorization("Bearer ${VedtaksdokumentTilgangStub.GODKJENT_TOKEN}")

    @AfterEach
    fun ryddOpp() = RequestContextHolder.resetRequestAttributes()

    @Test
    fun `manglende token gir 401`() {
        medAuthorization(null)

        shouldThrow<Feil> { vedtakDokumentController.dokumenterHentVedtaksdokumenter(nyttVedtakId().toString()) }
            .httpStatus shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `token uten påkrevd rolle gir 403`() {
        medAuthorization("Bearer ${VedtaksdokumentTilgangStub.UTEN_ROLLE_TOKEN}")

        shouldThrow<ForbiddenError> { vedtakDokumentController.dokumenterHentVedtaksdokumenter(nyttVedtakId().toString()) }
    }

    @Test
    fun `flere brev for en behandling og brev for andre behandlinger`() {
        val vedtakId = nyttVedtakId()
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val førsteBehandling = opprettBehandling(fagsak.id, vedtakId)
        val annenBehandling = opprettBehandling(fagsak.id, nyttVedtakId())
        val annenFagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandlingPåAnnenFagsak = opprettBehandling(annenFagsak.id, nyttVedtakId())
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(førsteBehandling, journalpostId = "journalpost-1", dokumentId = "dokument-1"),
        )
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(
                førsteBehandling,
                journalpostId = "journalpost-2",
                dokumentId = "dokument-2",
                brevtype = Brevtype.HENLEGGELSE,
            ),
        )
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(annenBehandling, journalpostId = "journalpost-3", dokumentId = "dokument-3"),
        )
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(behandlingPåAnnenFagsak, journalpostId = "journalpost-4", dokumentId = "dokument-4"),
        )

        val response = vedtakDokumentController.dokumenterHentVedtaksdokumenter(vedtakId.toString())

        response.statusCode shouldBe HttpStatus.OK
        response.body.shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(
            VedtaksdokumentDto(journalpostId = "journalpost-1", dokumentInfoId = "dokument-1"),
            VedtaksdokumentDto(journalpostId = "journalpost-2", dokumentInfoId = "dokument-2"),
        )
    }

    @Test
    fun `behandling uten brevsporing og brev for en annen behandling`() {
        val vedtakId = nyttVedtakId()
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        opprettBehandling(fagsak.id, vedtakId)
        val annenBehandling = opprettBehandling(fagsak.id, nyttVedtakId())
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(annenBehandling, journalpostId = "journalpost-annen", dokumentId = "dokument-annen"),
        )

        val response = vedtakDokumentController.dokumenterHentVedtaksdokumenter(vedtakId.toString())

        response.statusCode shouldBe HttpStatus.OK
        response.body.shouldNotBeNull() shouldBe emptyList<VedtaksdokumentDto>()
    }

    private fun medAuthorization(verdi: String?) {
        val request = MockHttpServletRequest()
        verdi?.let { request.addHeader("Authorization", it) }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    private fun opprettBehandling(fagsakId: UUID, vedtakId: BigInteger): UUID {
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id, vedtakId = vedtakId))
        return behandling.id
    }

    private fun nyttVedtakId(): BigInteger =
        BigInteger.valueOf(8_000_000_000_000_000_000L) + KravgrunnlagGenerator.nextId(18).toBigInteger()
}
