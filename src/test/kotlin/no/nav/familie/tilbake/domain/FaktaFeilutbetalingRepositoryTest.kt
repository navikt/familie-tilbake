package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.FaktaFeilutbetalingRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import no.nav.familie.tilbake.common.repository.findByIdOrThrow

internal class FaktaFeilutbetalingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    private val faktaFeilutbetaling = Testdata.faktaFeilutbetaling

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av FaktaFeilutbetaling til basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)

        val lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)

        Assertions.assertThat(lagretFaktaFeilutbetaling).isEqualToIgnoringGivenFields(faktaFeilutbetaling, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av FaktaFeilutbetaling i basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
        val oppdatertFaktaFeilutbetaling = faktaFeilutbetaling.copy(begrunnelse = "bob")

        faktaFeilutbetalingRepository.update(oppdatertFaktaFeilutbetaling)

        val lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)
        Assertions.assertThat(lagretFaktaFeilutbetaling).isEqualToIgnoringGivenFields(oppdatertFaktaFeilutbetaling, "sporbar")
    }

}