package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravvedtaksstatusRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class KravvedtaksstatusRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var kravvedtaksstatusRepository: KravvedtaksstatusRepository

    private val kravvedtaksstatus437 = Testdata.kravvedtaksstatus437

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Kravvedtaksstatus437 til basen`() {
        kravvedtaksstatusRepository.insert(kravvedtaksstatus437)

        val lagretKravvedtaksstatus437 = kravvedtaksstatusRepository.findByIdOrThrow(kravvedtaksstatus437.id)

        assertThat(lagretKravvedtaksstatus437).isEqualToIgnoringGivenFields(kravvedtaksstatus437,
                                                                            "sporbar", "versjon")
        assertEquals(1, lagretKravvedtaksstatus437.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Kravvedtaksstatus437 i basen`() {
        kravvedtaksstatusRepository.insert(kravvedtaksstatus437)
        var lagretKravvedtaksstatus437 = kravvedtaksstatusRepository.findByIdOrThrow(kravvedtaksstatus437.id)
        val oppdatertKravvedtaksstatus437 = lagretKravvedtaksstatus437.copy(fagsystemId = "bob")

        kravvedtaksstatusRepository.update(oppdatertKravvedtaksstatus437)

        lagretKravvedtaksstatus437 = kravvedtaksstatusRepository.findByIdOrThrow(kravvedtaksstatus437.id)
        assertThat(lagretKravvedtaksstatus437).isEqualToIgnoringGivenFields(oppdatertKravvedtaksstatus437,
                                                                            "sporbar", "versjon")
        assertEquals(2, lagretKravvedtaksstatus437.versjon)
    }

}
