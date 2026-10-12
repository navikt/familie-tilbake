package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.util.UUID

@Transactional
class KravgrunnlagVedtakRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `gammel modell returnerer distinkte behandlinger fra kravgrunnlag med samme vedtak`() {
        val vedtakId = nyttDokumentVedtakId()
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val førsteBehandling = opprettGammelBehandling(fagsak.id, vedtakId)
        val andreBehandling = opprettGammelBehandling(fagsak.id, vedtakId)
        kravgrunnlagRepository.insert(
            Testdata.lagKravgrunnlag(førsteBehandling, vedtakId = vedtakId, aktiv = false),
        )

        val behandlingIder = kravgrunnlagRepository.findBehandlingIdsByVedtakId(vedtakId)

        behandlingIder.size shouldBe 2
        behandlingIder.toSet() shouldBe setOf(førsteBehandling, andreBehandling)
    }

    private fun opprettGammelBehandling(fagsakId: UUID, vedtakId: BigInteger): UUID {
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id, vedtakId = vedtakId))
        return behandling.id
    }

    private fun nyttDokumentVedtakId(): BigInteger =
        BigInteger.valueOf(8_000_000_000_000_000_000L) + KravgrunnlagGenerator.nextId(18).toBigInteger()
}
