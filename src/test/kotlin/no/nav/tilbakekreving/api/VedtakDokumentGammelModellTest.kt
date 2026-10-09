package no.nav.tilbakekreving.api

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.e2e.ContextServiceHelpers.somSaksbehandler
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.util.UUID

@ActiveProfiles("ny-modell")
@Transactional
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

    @Test
    fun `flere brev for en behandling og brev for andre behandlinger i gammel modell`() {
        val vedtakId = nyttVedtakId()
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val førsteBehandling = opprettBehandling(fagsak.id, vedtakId)
        val annenBehandling = opprettBehandling(fagsak.id, nyttVedtakId())
        val annenFagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandlingPåAnnenFagsak = opprettBehandling(annenFagsak.id, nyttVedtakId())
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(førsteBehandling).copy(journalpostId = "journalpost-1", dokumentId = "dokument-1"),
        )
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(førsteBehandling).copy(
                journalpostId = "journalpost-2",
                dokumentId = "dokument-2",
                brevtype = Brevtype.HENLEGGELSE,
            ),
        )
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(annenBehandling).copy(journalpostId = "journalpost-3", dokumentId = "dokument-3"),
        )
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(behandlingPåAnnenFagsak).copy(journalpostId = "journalpost-4", dokumentId = "dokument-4"),
        )

        val respons = somSaksbehandler {
            vedtakDokumentController.dokumenterHentVedtaksdokumenter(vedtakId.toString())
        }

        respons.statusCode shouldBe HttpStatus.OK
        respons.body.shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(
            VedtaksdokumentDto(journalpostId = "journalpost-1", dokumentInfoId = "dokument-1"),
            VedtaksdokumentDto(journalpostId = "journalpost-2", dokumentInfoId = "dokument-2"),
        )
    }

    @Test
    fun `behandling uten brevsporing og brev for en annen behandling i gammel modell`() {
        val vedtakId = nyttVedtakId()
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        opprettBehandling(fagsak.id, vedtakId)
        val annenBehandling = opprettBehandling(fagsak.id, nyttVedtakId())
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(annenBehandling).copy(journalpostId = "journalpost-annen", dokumentId = "dokument-annen"),
        )

        val respons = somSaksbehandler {
            vedtakDokumentController.dokumenterHentVedtaksdokumenter(vedtakId.toString())
        }

        respons.statusCode shouldBe HttpStatus.OK
        respons.body.shouldNotBeNull() shouldBe emptyList<VedtaksdokumentDto>()
    }

    private fun opprettBehandling(fagsakId: UUID, vedtakId: BigInteger): UUID {
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id).copy(vedtakId = vedtakId))
        return behandling.id
    }

    private fun nyttVedtakId(): BigInteger =
        BigInteger.valueOf(8_000_000_000_000_000_000L) + KravgrunnlagGenerator.nextId(18).toBigInteger()
}
