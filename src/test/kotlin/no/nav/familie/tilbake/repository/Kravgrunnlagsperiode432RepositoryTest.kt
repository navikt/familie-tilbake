package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class Kravgrunnlagsperiode432RepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var kravgrunnlagsperiode432Repository: Kravgrunnlagsperiode432Repository

    @Autowired
    private lateinit var kravgrunnlag431Repository: Kravgrunnlag431Repository

    private val kravgrunnlagsperiode432 = Testdata.kravgrunnlagsperiode432

    @BeforeEach
    fun init() {
        kravgrunnlag431Repository.insert(Testdata.kravgrunnlag431)
    }

    @Test
    fun insertPersistererEnForekomstAvKravgrunnlagsperiode432TilBasen() {
        kravgrunnlagsperiode432Repository.insert(kravgrunnlagsperiode432)

        val lagretKravgrunnlagsperiode432 = kravgrunnlagsperiode432Repository.findByIdOrThrow(kravgrunnlagsperiode432.id)

        Assertions.assertThat(lagretKravgrunnlagsperiode432).isEqualToIgnoringGivenFields(kravgrunnlagsperiode432, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvKravgrunnlagsperiode432IBasen() {
        kravgrunnlagsperiode432Repository.insert(kravgrunnlagsperiode432)
        val oppdatertKravgrunnlagsperiode432 = kravgrunnlagsperiode432.copy(månedligSkattebeløp = 15423.28)

        kravgrunnlagsperiode432Repository.update(oppdatertKravgrunnlagsperiode432)

        val lagretKravgrunnlagsperiode432 = kravgrunnlagsperiode432Repository.findByIdOrThrow(kravgrunnlagsperiode432.id)
        Assertions.assertThat(lagretKravgrunnlagsperiode432)
                .isEqualToIgnoringGivenFields(oppdatertKravgrunnlagsperiode432, "sporbar")
    }

}