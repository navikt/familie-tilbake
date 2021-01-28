package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val fagsak = Testdata.fagsak

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Fagsak til basen`() {
        fagsakRepository.insert(fagsak)

        val lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        Assertions.assertThat(lagretFagsak).isEqualToIgnoringGivenFields(fagsak, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Fagsak i basen`() {
        fagsakRepository.insert(fagsak)
        val oppdatertFagsak = fagsak.copy(eksternFagsakId = "bob")

        fagsakRepository.update(oppdatertFagsak)

        val lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        Assertions.assertThat(lagretFagsak).isEqualToIgnoringGivenFields(oppdatertFagsak, "sporbar")
    }

}