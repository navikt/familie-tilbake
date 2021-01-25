package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.Hendelsestype
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
    fun insertPersistererEnForekomstAvFaktaFeilutbetalingsperiodeTilBasen() {
        faktaFeilutbetalingsperiodeRepository.insert(faktaFeilutbetalingsperiode)

        val lagretFaktaFeilutbetalingsperiode =
                faktaFeilutbetalingsperiodeRepository.findByIdOrThrow(faktaFeilutbetalingsperiode.id)

        Assertions.assertThat(lagretFaktaFeilutbetalingsperiode)
                .isEqualToIgnoringGivenFields(faktaFeilutbetalingsperiode, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvFaktaFeilutbetalingsperiodeIBasen() {
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