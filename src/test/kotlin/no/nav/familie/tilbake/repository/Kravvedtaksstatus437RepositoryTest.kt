package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class Kravvedtaksstatus437RepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var kravvedtaksstatus437Repository: Kravvedtaksstatus437Repository

    private val kravvedtaksstatus437 = Testdata.kravvedtaksstatus437

    @Test
    fun insertPersistererEnForekomstAvKravvedtaksstatus437TilBasen() {
        kravvedtaksstatus437Repository.insert(kravvedtaksstatus437)

        val lagretKravvedtaksstatus437 = kravvedtaksstatus437Repository.findByIdOrThrow(kravvedtaksstatus437.id)

        Assertions.assertThat(lagretKravvedtaksstatus437).isEqualToIgnoringGivenFields(kravvedtaksstatus437, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvKravvedtaksstatus437IBasen() {
        kravvedtaksstatus437Repository.insert(kravvedtaksstatus437)
        val oppdatertKravvedtaksstatus437 = kravvedtaksstatus437.copy(fagsystemId = "bob")

        kravvedtaksstatus437Repository.update(oppdatertKravvedtaksstatus437)

        val lagretKravvedtaksstatus437 = kravvedtaksstatus437Repository.findByIdOrThrow(kravvedtaksstatus437.id)
        Assertions.assertThat(lagretKravvedtaksstatus437).isEqualToIgnoringGivenFields(oppdatertKravvedtaksstatus437, "sporbar")
    }

}