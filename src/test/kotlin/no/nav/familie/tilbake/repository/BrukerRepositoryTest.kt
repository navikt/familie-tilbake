package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BrukerRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var brukerRepository: BrukerRepository

    private val bruker = Testdata.bruker

    @Test
    fun insertPersistererEnForekomstAvBrukerTilBasen() {
        brukerRepository.insert(bruker)

        val lagretBruker = brukerRepository.findByIdOrThrow(bruker.id)
        Assertions.assertThat(lagretBruker).isEqualToIgnoringGivenFields(bruker, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvBrukerIBasen() {
        brukerRepository.insert(bruker)
        val oppdatertBruker = bruker.copy(ident = "bob")

        brukerRepository.update(oppdatertBruker)

        val lagretBruker = brukerRepository.findByIdOrThrow(bruker.id)
        Assertions.assertThat(lagretBruker).isEqualToIgnoringGivenFields(oppdatertBruker, "sporbar")
    }

}