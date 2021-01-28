package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.repository.tbd.FaktaFeilutbetalingsperiodeRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.domain.tbd.Hendelsestype

internal class FaktaFeilutbetalingsperiodeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var faktaFeilutbetalingsperiodeRepository: FaktaFeilutbetalingsperiodeRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    private val faktaFeilutbetalingsperiode = Testdata.faktaFeilutbetalingsperiode

    @BeforeEach
    fun init() {
        faktaFeilutbetalingRepository.insert(Testdata.faktaFeilutbetaling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av FaktaFeilutbetalingsperiode til basen`() {
        faktaFeilutbetalingsperiodeRepository.insert(faktaFeilutbetalingsperiode)

        val lagretFaktaFeilutbetalingsperiode =
                faktaFeilutbetalingsperiodeRepository.findByIdOrThrow(faktaFeilutbetalingsperiode.id)

        Assertions.assertThat(lagretFaktaFeilutbetalingsperiode)
                .isEqualToIgnoringGivenFields(faktaFeilutbetalingsperiode, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av FaktaFeilutbetalingsperiode i basen`() {
        faktaFeilutbetalingsperiodeRepository.insert(faktaFeilutbetalingsperiode)
        val oppdatertFaktaFeilutbetalingsperiode = faktaFeilutbetalingsperiode.copy(hendelsestype = Hendelsestype.EF_ANNET)

        faktaFeilutbetalingsperiodeRepository.update(oppdatertFaktaFeilutbetalingsperiode)

        val lagretFaktaFeilutbetalingsperiode =
                faktaFeilutbetalingsperiodeRepository.findByIdOrThrow(faktaFeilutbetalingsperiode.id)
        Assertions.assertThat(lagretFaktaFeilutbetalingsperiode)
                .isEqualToIgnoringGivenFields(oppdatertFaktaFeilutbetalingsperiode,
                                              "sporbar")
    }

}