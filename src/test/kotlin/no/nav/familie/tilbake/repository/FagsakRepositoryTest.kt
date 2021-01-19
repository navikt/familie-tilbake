package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val fagsak = Testdata.fagsak

    @Test
    fun insertPersistererEnForekomstAvFagsakTilBasen() {
        fagsakRepository.insert(fagsak)

        val lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        Assertions.assertThat(lagretFagsak).isEqualToIgnoringGivenFields(fagsak, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvFagsakIBasen() {
        fagsakRepository.insert(fagsak)
        val oppdatertFagsak = fagsak.copy(eksternFagsakId = "bob")

        fagsakRepository.update(oppdatertFagsak)

        val lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        Assertions.assertThat(lagretFagsak).isEqualToIgnoringGivenFields(oppdatertFagsak, "sporbar")
    }

}