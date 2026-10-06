package no.nav.familie.tilbake.dokumentbestilling.felles

import io.kotest.inspectors.forAll
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class BrevsporingVedtakDokumentRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var repository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `flere brev for en behandling og brev for en annen behandling`() {
        val førsteBehandling = opprettBehandling()
        val annenBehandling = opprettBehandling()
        val førsteBrev = repository.insert(
            Testdata.lagBrevsporing(førsteBehandling).copy(journalpostId = "journalpost-1", dokumentId = "dokument-1"),
        )
        val andreBrev = repository.insert(
            Testdata.lagBrevsporing(førsteBehandling).copy(
                journalpostId = "journalpost-2",
                dokumentId = "dokument-2",
                brevtype = Brevtype.HENLEGGELSE,
            ),
        )
        repository.insert(Testdata.lagBrevsporing(annenBehandling))

        val brev = repository.findAllByBehandlingId(førsteBehandling)

        val forventedeBrev = listOf(førsteBrev, andreBrev)
        brev.map { it.id }.toSet() shouldBe forventedeBrev.map { it.id }.toSet()
        brev.size shouldBe forventedeBrev.size
        brev.forAll { lagretBrev ->
            lagretBrev.shouldBeEqualToIgnoringFields(
                forventedeBrev.single { it.id == lagretBrev.id },
                Brevsporing::sporbar,
            )
        }
    }

    @Test
    fun `behandling uten brevsporing`() {
        val behandlingId = opprettBehandling()
        repository.insert(Testdata.lagBrevsporing(opprettBehandling()))

        repository.findAllByBehandlingId(behandlingId) shouldBe emptyList()
    }

    private fun opprettBehandling(): UUID {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        return behandlingRepository.insert(Testdata.lagBehandling(fagsak.id)).id
    }
}
