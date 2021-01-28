package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.Kravvedtaksstatus437Repository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import no.nav.familie.tilbake.common.repository.findByIdOrThrow

internal class Kravvedtaksstatus437RepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var kravvedtaksstatus437Repository: Kravvedtaksstatus437Repository

    private val kravvedtaksstatus437 = Testdata.kravvedtaksstatus437

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Kravvedtaksstatus437 til basen`() {
        kravvedtaksstatus437Repository.insert(kravvedtaksstatus437)

        val lagretKravvedtaksstatus437 = kravvedtaksstatus437Repository.findByIdOrThrow(kravvedtaksstatus437.id)

        Assertions.assertThat(lagretKravvedtaksstatus437).isEqualToIgnoringGivenFields(kravvedtaksstatus437, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Kravvedtaksstatus437 i basen`() {
        kravvedtaksstatus437Repository.insert(kravvedtaksstatus437)
        val oppdatertKravvedtaksstatus437 = kravvedtaksstatus437.copy(fagsystemId = "bob")

        kravvedtaksstatus437Repository.update(oppdatertKravvedtaksstatus437)

        val lagretKravvedtaksstatus437 = kravvedtaksstatus437Repository.findByIdOrThrow(kravvedtaksstatus437.id)
        Assertions.assertThat(lagretKravvedtaksstatus437).isEqualToIgnoringGivenFields(oppdatertKravvedtaksstatus437, "sporbar")
    }

}