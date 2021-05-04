package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val fagsak = Testdata.fagsak

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Fagsak til basen`() {
        fagsakRepository.insert(fagsak)

        val lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        assertThat(lagretFagsak).usingRecursiveComparison().ignoringFields("sporbar", "versjon").isEqualTo(fagsak)
        assertEquals(1, lagretFagsak.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Fagsak i basen`() {
        fagsakRepository.insert(fagsak)
        var lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        val oppdatertFagsak = lagretFagsak.copy(eksternFagsakId = "1")

        fagsakRepository.update(oppdatertFagsak)

        lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        assertThat(lagretFagsak).usingRecursiveComparison().ignoringFields("sporbar", "versjon").isEqualTo(oppdatertFagsak)
        assertEquals(2, lagretFagsak.versjon)
    }

}
