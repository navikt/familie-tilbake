package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.Fagsystem
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class Kravgrunnlag431RepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var kravgrunnlag431Repository: Kravgrunnlag431Repository

    private val kravgrunnlag431 = Testdata.kravgrunnlag431

    @Test
    fun insertPersistererEnForekomstAvKravgrunnlag431TilBasen() {
        kravgrunnlag431Repository.insert(kravgrunnlag431)

        val lagretKravgrunnlag431 = kravgrunnlag431Repository.findByIdOrThrow(kravgrunnlag431.id)

        Assertions.assertThat(lagretKravgrunnlag431).isEqualToIgnoringGivenFields(kravgrunnlag431, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvKravgrunnlag431IBasen() {
        kravgrunnlag431Repository.insert(kravgrunnlag431)
        val oppdatertKravgrunnlag431 = kravgrunnlag431.copy(fagsystem = Fagsystem.GOSYS)

        kravgrunnlag431Repository.update(oppdatertKravgrunnlag431)

        val lagretKravgrunnlag431 = kravgrunnlag431Repository.findByIdOrThrow(kravgrunnlag431.id)
        Assertions.assertThat(lagretKravgrunnlag431).isEqualToIgnoringGivenFields(oppdatertKravgrunnlag431, "sporbar")
    }

}