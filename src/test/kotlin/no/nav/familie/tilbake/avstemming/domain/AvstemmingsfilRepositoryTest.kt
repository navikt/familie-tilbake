package no.nav.familie.tilbake.avstemming.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class AvstemmingsfilRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var avstemmingsfilRepository: AvstemmingsfilRepository

    private val avstemmingsfil = Testdata.avstemmingsfil

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Avstemmingsfil til basen`() {
        avstemmingsfilRepository.insert(avstemmingsfil)

        val lagretAvstemmingsfil = avstemmingsfilRepository.findByIdOrThrow(avstemmingsfil.id)

        assertThat(lagretAvstemmingsfil).usingRecursiveComparison().ignoringFields("sporbar", "versjon").isEqualTo(avstemmingsfil)
        assertEquals(1, lagretAvstemmingsfil.versjon)
    }

}
