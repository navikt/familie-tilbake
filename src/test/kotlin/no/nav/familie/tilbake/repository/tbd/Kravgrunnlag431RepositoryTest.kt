package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.tbd.Fagsystem
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class Kravgrunnlag431RepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var kravgrunnlag431Repository: Kravgrunnlag431Repository

    private val kravgrunnlag431 = Testdata.kravgrunnlag431

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Kravgrunnlag431 til basen`() {
        kravgrunnlag431Repository.insert(kravgrunnlag431)

        val lagretKravgrunnlag431 = kravgrunnlag431Repository.findByIdOrThrow(kravgrunnlag431.id)

        Assertions.assertThat(lagretKravgrunnlag431).isEqualToIgnoringGivenFields(kravgrunnlag431, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Kravgrunnlag431 i basen`() {
        kravgrunnlag431Repository.insert(kravgrunnlag431)
        val oppdatertKravgrunnlag431 = kravgrunnlag431.copy(fagsystem = Fagsystem.GOSYS)

        kravgrunnlag431Repository.update(oppdatertKravgrunnlag431)

        val lagretKravgrunnlag431 = kravgrunnlag431Repository.findByIdOrThrow(kravgrunnlag431.id)
        Assertions.assertThat(lagretKravgrunnlag431).isEqualToIgnoringGivenFields(oppdatertKravgrunnlag431, "sporbar")
    }

}
