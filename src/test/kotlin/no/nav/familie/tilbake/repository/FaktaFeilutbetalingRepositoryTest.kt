package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FaktaFeilutbetalingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    private val faktaFeilutbetaling = Testdata.faktaFeilutbetaling

    @Test
    fun insertPersistererEnForekomstAvFaktaFeilutbetalingTilBasen() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)

        val lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)

        Assertions.assertThat(lagretFaktaFeilutbetaling).isEqualToIgnoringGivenFields(faktaFeilutbetaling, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvFaktaFeilutbetalingIBasen() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
        val oppdatertFaktaFeilutbetaling = faktaFeilutbetaling.copy(begrunnelse = "bob")

        faktaFeilutbetalingRepository.update(oppdatertFaktaFeilutbetaling)

        val lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)
        Assertions.assertThat(lagretFaktaFeilutbetaling).isEqualToIgnoringGivenFields(oppdatertFaktaFeilutbetaling, "sporbar")
    }

}