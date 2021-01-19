package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VergeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vergeRepository: VergeRepository

    private val verge = Testdata.verge

    @Test
    fun insertPersistererEnForekomstAvVergeTilBasen() {
        vergeRepository.insert(verge)

        val lagretVerge = vergeRepository.findByIdOrThrow(verge.id)

        Assertions.assertThat(lagretVerge).isEqualToIgnoringGivenFields(verge, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVergeIBasen() {
        vergeRepository.insert(verge)
        val oppdatertVerge = verge.copy(navn = "bob")

        vergeRepository.update(oppdatertVerge)

        val lagretVerge = vergeRepository.findByIdOrThrow(verge.id)
        Assertions.assertThat(lagretVerge).isEqualToIgnoringGivenFields(oppdatertVerge, "sporbar")
    }

}