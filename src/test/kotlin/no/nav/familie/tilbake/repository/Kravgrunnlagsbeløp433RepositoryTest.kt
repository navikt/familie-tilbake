package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class Kravgrunnlagsbeløp433RepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var kravgrunnlagsbeløp433Repository: Kravgrunnlagsbeløp433Repository

    @Autowired
    private lateinit var kravgrunnlagsperiode432Repository: Kravgrunnlagsperiode432Repository

    @Autowired
    private lateinit var kravgrunnlag431Repository: Kravgrunnlag431Repository

    private val kravgrunnlagsbeløp433 = Testdata.kravgrunnlagsbeløp433

    @BeforeEach
    fun init() {
        kravgrunnlag431Repository.insert(Testdata.kravgrunnlag431)
        kravgrunnlagsperiode432Repository.insert(Testdata.kravgrunnlagsperiode432)
    }

    @Test
    fun insertPersistererEnForekomstAvKravgrunnlagsbeløp433TilBasen() {
        kravgrunnlagsbeløp433Repository.insert(kravgrunnlagsbeløp433)

        val lagretKravgrunnlagsbeløp433 = kravgrunnlagsbeløp433Repository.findByIdOrThrow(kravgrunnlagsbeløp433.id)

        Assertions.assertThat(lagretKravgrunnlagsbeløp433).isEqualToIgnoringGivenFields(kravgrunnlagsbeløp433, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvKravgrunnlagsbeløp433IBasen() {
        kravgrunnlagsbeløp433Repository.insert(kravgrunnlagsbeløp433)
        val oppdatertKravgrunnlagsbeløp433 = kravgrunnlagsbeløp433.copy(klassekode = "bob")

        kravgrunnlagsbeløp433Repository.update(oppdatertKravgrunnlagsbeløp433)

        val lagretKravgrunnlagsbeløp433 = kravgrunnlagsbeløp433Repository.findByIdOrThrow(kravgrunnlagsbeløp433.id)
        Assertions.assertThat(lagretKravgrunnlagsbeløp433).isEqualToIgnoringGivenFields(oppdatertKravgrunnlagsbeløp433, "sporbar")
    }

}